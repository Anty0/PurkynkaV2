/*
 * app
 * Copyright (C)   2018  anty
 *
 * This program is free  software: you can redistribute it and/or modify
 * it under the terms  of the GNU General Public License as published by
 * the Free Software  Foundation, either version 3 of the License, or
 * (at your option) any  later version.
 *
 * This program is distributed in the hope that it  will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied  warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.   See the
 * GNU General Public License for more details.
 *
 * You  should have received a copy of the GNU General Public License
 * along  with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cz.anty.purkynka.lunches.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import cz.anty.purkynka.R
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.lunches.data.LunchBurza
import cz.anty.purkynka.lunches.data.LunchBurza.Companion.dateStrShort
import cz.anty.purkynka.lunches.load.LunchesFetcher
import cz.anty.purkynka.lunches.load.LunchesParser
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesLoginData
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.getKSerializableExtra
import eu.codetopic.utils.putKSerializableExtra
import eu.codetopic.utils.ui.activity.modular.module.CoordinatorLayoutModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.activity.modular.module.TransitionBackButtonModule
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.view.holder.loading.LoadingModularActivity
import kotlinx.android.synthetic.main.activity_lunch_burza.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.ctx
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.io.IOException

/**
 * @author anty
 */
class LunchBurzaActivity : LoadingModularActivity(
        CoordinatorLayoutModule(), ToolbarModule(), TransitionBackButtonModule()
) {

    companion object {

        private const val LOG_TAG = "LunchBurzaActivity"

        private const val EXTRA_ACCOUNT_ID =
                "cz.anty.purkynka.lunches.ui.$LOG_TAG.EXTRA_ACCOUNT_ID"
        private const val EXTRA_LUNCH_BURZA =
                "cz.anty.purkynka.lunches.ui.$LOG_TAG.EXTRA_LUNCH_BURZA"

        fun getStartIntent(context: Context, accountId: String, lunchBurza: LunchBurza) =
                Intent(context, LunchOptionsGroupActivity::class.java)
                        .putExtra(EXTRA_ACCOUNT_ID, accountId)
                        .putKSerializableExtra(EXTRA_LUNCH_BURZA, lunchBurza)

        fun start(context: Context, accountId: String, lunchesLunchBurza: LunchBurza) =
                context.startActivity(getStartIntent(context, accountId, lunchesLunchBurza))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lunch_burza)

        val accountId = intent?.getStringExtra(EXTRA_ACCOUNT_ID)
                ?:
                run {
                    Log.e(LOG_TAG, "Can't create $LOG_TAG: No AccountId received")
                    finish()
                    return
                }
        val lunchBurza = intent
                ?.getKSerializableExtra<LunchBurza>(EXTRA_LUNCH_BURZA)
                ?:
                run {
                    Log.e(LOG_TAG, "Can't create $LOG_TAG: No LunchBurza received")
                    finish()
                    return
                }

        title = getFormattedText(
                R.string.title_activity_lunches_burza_lunch_with_date,
                lunchBurza.dateStrShort
        )

        val lunchBurzaItem = LunchBurzaItem(accountId, lunchBurza)


        val itemVH = lunchBurzaItem.createViewHolder(this, boxLunchBurza)
                .also { boxLunchBurza.addView(it.itemView, 0) }
        lunchBurzaItem.bindViewHolder(itemVH, CustomItem.NO_POSITION)

        val self = this.asReference()
        butOrder.onClick {
            holder.showLoading()

            val success = bg {
                try {
                    val loginData = LunchesLoginData.loginData

                    if (!loginData.isLoggedIn(accountId))
                        throw IllegalStateException("User is not logged in")

                    val (username, password) = loginData.getCredentials(accountId)

                    if (username == null || password == null)
                        throw IllegalStateException("Username or password is null")

                    val cookies = LunchesFetcher.login(username, password)

                    if (!LunchesFetcher.isLoggedIn(cookies))
                        throw WrongLoginDataException("Failed to login user with provided credentials")

                    val lunchesHtml = LunchesFetcher.getLunchesBurzaElements(cookies)
                    val burzaLunches = LunchesParser.parseLunchesBurza(lunchesHtml)
                    val nLunchBurza = burzaLunches.indexOf(lunchBurza)
                            .takeIf { it != -1 }
                            ?.let { burzaLunches[it] }
                            ?: throw IllegalStateException("Lunch burza not found")

                    val url = nLunchBurza.orderUrl

                    LunchesFetcher.orderLunch(cookies, url)

                    LunchesFetcher.logout(cookies)

                    LunchesData.instance.invalidateData(accountId)
                    return@bg true
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "butOrder.onClick(" +
                            "lunchBurza=$lunchBurza)" +
                            " -> Failed to order lunch", e)

                    launch(UI) {
                        longSnackbar(self().boxLunchBurza, when (e) {
                            is WrongLoginDataException -> R.string.snackbar_lunches_order_fail_login
                            is IOException -> R.string.snackbar_lunches_order_fail_connect
                            else -> R.string.snackbar_lunches_order_fail_unknown
                        })
                    }
                    return@bg false
                }
            }.await()

            if (success) self().finish()
            holder.hideLoading()
        }
    }

    override fun onResume() {
        super.onResume()

        boxLunchBurzaInfo.takeIf { it.visibility == View.GONE }
                ?.apply {
                    visibility = View.VISIBLE
                    startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.slide_down))
                }
    }

    override fun finishAfterTransition() {
        // fixes bug in Android M (5), that crashes application
        //  if shared element is missing in previous activity.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) super.finishAfterTransition()
        else finish()
    }
}
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
import android.transition.Transition
import android.view.View
import android.view.animation.AnimationUtils
import cz.anty.purkynka.R
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.getKSerializableExtra
import eu.codetopic.utils.AndroidExtensions.putKSerializableExtra
import eu.codetopic.utils.simple.SimpleTransitionListener
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.activity.modular.module.TransitionBackButtonModule
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import kotlinx.android.synthetic.main.activity_lunch_options_group.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onCheckedChange
import org.jetbrains.anko.sdk25.coroutines.onClick
import android.widget.RadioButton
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.lunches.data.LunchOption
import cz.anty.purkynka.lunches.load.LunchesFetcher
import cz.anty.purkynka.lunches.load.LunchesParser
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesLoginData
import eu.codetopic.utils.ui.view.holder.loading.LoadingModularActivity
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.design.longSnackbar
import java.io.IOException


/**
 * @author anty
 */
class LunchOptionsGroupActivity : LoadingModularActivity(ToolbarModule(), TransitionBackButtonModule()) {

    companion object {

        private const val LOG_TAG = "LunchOptionsGroupActivity"

        private const val EXTRA_ACCOUNT_ID =
                "cz.anty.purkynka.lunches.ui.$LOG_TAG.EXTRA_ACCOUNT_ID"
        private const val EXTRA_LUNCH_OPTIONS_GROUP =
                "cz.anty.purkynka.lunches.ui.$LOG_TAG.EXTRA_LUNCH_OPTIONS_GROUP"

        fun getStartIntent(context: Context, accountId: String, lunchOptionsGroup: LunchOptionsGroup) =
                Intent(context, LunchOptionsGroupActivity::class.java)
                        .putExtra(EXTRA_ACCOUNT_ID, accountId)
                        .putKSerializableExtra(EXTRA_LUNCH_OPTIONS_GROUP, lunchOptionsGroup)

        fun start(context: Context, accountId: String, lunchOptionsGroup: LunchOptionsGroup) =
                context.startActivity(getStartIntent(context, accountId, lunchOptionsGroup))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lunch_options_group)

        val accountId = intent?.getStringExtra(EXTRA_ACCOUNT_ID)
                ?:
                run {
                    Log.e(LOG_TAG, "Can't create $LOG_TAG: No AccountId received")
                    finish()
                    return
                }
        val lunchOptionsGroup = intent
                ?.getKSerializableExtra<LunchOptionsGroup>(EXTRA_LUNCH_OPTIONS_GROUP)
                ?:
                run {
                    Log.e(LOG_TAG, "Can't create $LOG_TAG: No LunchOptionsGroup received")
                    finish()
                    return
                }

        //title = lunchOptionsGroup.? //TODO: maybe set title to something

        val lunchOptionsGroupItem = LunchOptionsGroupItem(accountId, lunchOptionsGroup)

        val itemVH = lunchOptionsGroupItem.createViewHolder(this, boxLunchOptionsGroup)
                .also { boxLunchOptionsGroup.addView(it.itemView, 0) }
        lunchOptionsGroupItem.bindViewHolder(itemVH, CustomItem.NO_POSITION)

        // TODO: if lunch is not ordered and can't be ordered, check burza for available lunches and show info to user.

        boxOptionsGroup.apply {
            val butNoLunch = radioButton {
                tag = null
                textResource = R.string.but_no_lunch
                id = 0
                onCheckedChange { _, isChecked ->
                    if (isChecked) butLunchOrder.isEnabled = this@radioButton.tag != null
                }
            }

            var toCheck = butNoLunch.id

            lunchOptionsGroup.options?.forEachIndexed { index, lunchOption ->
                radioButton {
                    tag = lunchOption.takeUnless { it.ordered }
                    text = lunchOption.name
                    id = index + 1
                    isEnabled = lunchOption.enabled

                    if (lunchOption.ordered) {
                        if (lunchOption.enabled) {
                            butNoLunch.tag = lunchOption
                        }
                        toCheck = this.id
                    }

                    onCheckedChange { _, isChecked ->
                        if (isChecked) butLunchOrder.isEnabled = this@radioButton.tag != null
                    }
                }
            }

            butNoLunch.takeIf { it.tag == null && it.id != toCheck }?.isEnabled = false
            check(toCheck)
        }

        butLunchOrder.onClick {
            val lunchOptionToOrder = boxOptionsGroup
                    .findViewById<RadioButton>(
                            boxOptionsGroup.checkedRadioButtonId
                    ).tag as? LunchOption ?: run {
                longSnackbar(boxLunchOptionsGroup, R.string.snackbar_lunches_order_fail_internal)
                return@onClick
            }
            val lunchOptionIndex = lunchOptionsGroup.options?.indexOf(lunchOptionToOrder) ?: run {
                longSnackbar(boxLunchOptionsGroup, R.string.snackbar_lunches_order_fail_internal)
                return@onClick
            }

            /*val url = lunchOptionToOrder.orderOrCancelUrl ?: run {
                longSnackbar(boxLunchOptionsGroup, R.string.snackbar_lunches_order_fail_internal)
                return@onClick
            }*/

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

                    val lunchHtml = LunchesFetcher.getLunchOptionsGroupElement(cookies, lunchOptionsGroup.date)
                    val nLunchOptionsGroup = LunchesParser.parseLunchOptionsGroup(lunchHtml)
                            .takeIf { it == lunchOptionsGroup }
                            ?: throw IllegalStateException("Lunch options group to order not found")
                    val nLunchOptionToOrder = nLunchOptionsGroup.options?.get(lunchOptionIndex)
                            .takeIf {
                                it == lunchOptionToOrder
                                        && it.enabled == lunchOptionToOrder.enabled
                                        && it.ordered == lunchOptionToOrder.ordered
                            }
                            ?: throw IllegalStateException("Lunch option to order not found")

                    val url = nLunchOptionToOrder.orderOrCancelUrl
                            ?: throw IllegalStateException("Lunch option url to order not found")

                    LunchesFetcher.orderLunch(cookies, url)

                    LunchesFetcher.logout(cookies)

                    LunchesData.instance.invalidateData(accountId)
                    return@bg true
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "butLunchOrder.onClick(" +
                            "lunchOptionToOrder=$lunchOptionToOrder)" +
                            " -> Failed to order lunch", e)

                    longSnackbar(boxLunchOptionsGroup, when (e) {
                        is WrongLoginDataException -> R.string.snackbar_lunches_order_fail_login
                        is IOException -> R.string.snackbar_lunches_order_fail_connect
                        else -> R.string.snackbar_lunches_order_fail_unknown
                    })
                    return@bg false
                }
            }.await()

            if (success) finish()
            holder.hideLoading()
        }

        lunchOptionsGroup.orderedOption
                .takeIf { it?.second?.isInBurza != null }
                ?.also {
                    val (index, lunchOption) = it
                    val isInBurza = lunchOption.isInBurza == true

                    boxToOrFromBurza.visibility = View.VISIBLE

                    val button =
                            if (isInBurza) butLunchFromBurza
                            else butLunchToBurza

                    button.apply {
                        visibility = View.VISIBLE
                        onClick {
                            /*val url = lunchOption.toOrFromBurzaUrl ?: run {
                                longSnackbar(
                                        boxLunchOptionsGroup,
                                        if (isInBurza) R.string.snackbar_lunches_from_burza_fail_internal
                                        else R.string.snackbar_lunches_to_burza_fail_internal
                                )
                                return@onClick
                            }*/

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

                                    val lunchHtml = LunchesFetcher.getLunchOptionsGroupElement(cookies, lunchOptionsGroup.date)
                                    val nLunchOptionsGroup = LunchesParser.parseLunchOptionsGroup(lunchHtml)
                                            .takeIf { it == lunchOptionsGroup }
                                            ?: throw IllegalStateException("Lunch options group to order not found")
                                    val nLunchOption = nLunchOptionsGroup.options?.get(index)
                                            .takeIf { it == lunchOption && it.isInBurza == lunchOption.isInBurza }
                                            ?: throw IllegalStateException("Lunch option to order not found")

                                    val url = nLunchOption.toOrFromBurzaUrl
                                            ?: throw IllegalStateException("Lunch option url to order not found")

                                    LunchesFetcher.orderLunch(cookies, url)

                                    LunchesFetcher.logout(cookies)

                                    LunchesData.instance.invalidateData(accountId)
                                    return@bg true
                                } catch (e: Exception) {
                                    Log.w(LOG_TAG, "butLunchToOrFromBurza.onClick(" +
                                            "lunchOption=$lunchOption)" +
                                            " -> Failed to move lunch from or to burza", e)

                                    longSnackbar(boxLunchOptionsGroup, when (e) {
                                        is WrongLoginDataException ->
                                            if (isInBurza) R.string.snackbar_lunches_from_burza_fail_login
                                            else R.string.snackbar_lunches_to_burza_fail_login
                                        is IOException ->
                                            if (isInBurza) R.string.snackbar_lunches_from_burza_fail_connect
                                            else R.string.snackbar_lunches_to_burza_fail_connect
                                        else ->
                                            if (isInBurza) R.string.snackbar_lunches_from_burza_fail_unknown
                                            else R.string.snackbar_lunches_to_burza_fail_unknown
                                    })
                                    return@bg false
                                }
                            }.await()

                            if (success) finish()
                            holder.hideLoading()
                        }
                    }
                }

        if (savedInstanceState == null) {
            val lunchOptionsGroupInfoAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            val showLunchOptionsGroupInfo = {
                boxLunchOptionsGroupInfo.apply {
                    visibility = View.VISIBLE
                    startAnimation(lunchOptionsGroupInfoAnimation)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.sharedElementEnterTransition?.apply {
                    addListener(object : SimpleTransitionListener() {
                        override fun onTransitionEnd(transition: Transition) {
                            run(showLunchOptionsGroupInfo)
                        }
                    })
                } ?: run(showLunchOptionsGroupInfo)
            } else run(showLunchOptionsGroupInfo)
        } else {
            boxLunchOptionsGroupInfo.visibility = View.VISIBLE
        }
    }
}
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
import cz.anty.purkynka.lunches.data.LunchBurza
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.getKSerializableExtra
import eu.codetopic.utils.AndroidExtensions.putKSerializableExtra
import eu.codetopic.utils.simple.SimpleTransitionListener
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.activity.modular.module.TransitionBackButtonModule
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.view.holder.loading.LoadingModularActivity
import kotlinx.android.synthetic.main.activity_lunch_burza.*
import org.jetbrains.anko.sdk25.coroutines.onClick

/**
 * @author anty
 */
class LunchBurzaActivity : LoadingModularActivity(ToolbarModule(), TransitionBackButtonModule()) {

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

        val lunchBurzaItem = LunchBurzaItem(accountId, lunchBurza)


        val itemVH = lunchBurzaItem.createViewHolder(this, boxLunchBurza)
                .also { boxLunchBurza.addView(it.itemView, 0) }
        lunchBurzaItem.bindViewHolder(itemVH, CustomItem.NO_POSITION)

        butOrder.onClick {
            // TODO: implement
        }

        if (savedInstanceState == null) {
            val lunchBurzaInfoAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            val showLunchBurzaInfo = {
                boxLunchBurzaInfo.apply {
                    visibility = View.VISIBLE
                    startAnimation(lunchBurzaInfoAnimation)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.sharedElementEnterTransition?.apply {
                    addListener(object : SimpleTransitionListener() {
                        override fun onTransitionEnd(transition: Transition) {
                            run(showLunchBurzaInfo)
                        }
                    })
                } ?: run(showLunchBurzaInfo)
            } else run(showLunchBurzaInfo)
        } else {
            boxLunchBurzaInfo.visibility = View.VISIBLE
        }
    }
}
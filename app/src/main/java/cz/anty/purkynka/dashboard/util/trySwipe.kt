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

package cz.anty.purkynka.dashboard.util

import android.content.Context
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.dashboard.DashboardItem
import cz.anty.purkynka.dashboard.DashboardManager
import cz.anty.purkynka.dashboard.SwipeableDashboardItem
import cz.anty.purkynka.settings.AppPreferences
import cz.anty.purkynka.utils.DASHBOARD_PRIORITY_TRY_SWIPE
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.receiver
import eu.codetopic.utils.ui.container.adapter.MultiAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import android.animation.ObjectAnimator
import android.view.View
import android.widget.TextView
import eu.codetopic.utils.ui.view.ViewUtils
import eu.codetopic.utils.ui.view.getTag
import eu.codetopic.utils.ui.view.setTag
import kotlinx.android.synthetic.main.item_dashboard_try_swipe.view.*
import android.animation.Animator
import android.widget.ImageView
import eu.codetopic.utils.baseActivity
import eu.codetopic.utils.simple.SimpleAnimatorListener


/**
 * @author anty
 */
class TrySwipeDashboardManager(context: Context, accountHolder: ActiveAccountHolder,
                               adapter: MultiAdapter<DashboardItem>) :
        DashboardManager(context, accountHolder, adapter) {

    companion object {

        private const val LOG_TAG = "TrySwipeDashboardManager"
        private const val ID = "cz.anty.purkynka.dashboard.util.trySwipe"
    }

    private val updateReceiver = receiver { _, _ -> update() }

    override fun register(): Job? {
        LocalBroadcast.registerReceiver(
                receiver = updateReceiver,
                filter = intentFilter(
                        AppPreferences.getter
                )
        )

        return update()
    }

    override fun unregister(): Job? {
        LocalBroadcast.unregisterReceiver(updateReceiver)

        return null
    }

    override fun update(): Job? {
        val adapterRef = adapter.asReference()

        return launch(UI) {
            adapterRef().mapReplaceAll(
                    id = ID,
                    items = bg calcItems@ {
                        val showItem = AppPreferences.instance.showTrySwipeItem
                        if (!showItem) return@calcItems null

                        return@calcItems listOf(
                                TrySwipeDashboardItem()
                        )
                    }.await() ?: emptyList()
            )
        }
    }
}

class TrySwipeDashboardItem() : SwipeableDashboardItem() {

    companion object {

        private const val LOG_TAG = "TrySwipeDashboardItem"
        private const val TAG_ITEM_READY = "cz.anty.purkynka.dashboard.util.$LOG_TAG.ITEM_READY"
    }

    override val priority: Int
        get() = DASHBOARD_PRIORITY_TRY_SWIPE

    override fun getSwipeDirections(holder: CustomItemViewHolder): Int = LEFT or RIGHT

    override fun onSwiped(holder: CustomItemViewHolder, direction: Int) {
        bg { AppPreferences.instance.showTrySwipeItem = false }
    }

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {
        val tag = holder.itemView.getTag(TAG_ITEM_READY)
        if (tag == null || tag !is Boolean || !tag) {
            holder.itemView.setTag(TAG_ITEM_READY, true)

            val img = holder.itemView.imgTrySwipe
            val txt = holder.itemView.txtTrySwipe.apply { alpha = 0f }

            ObjectAnimator
                    .ofFloat(img, View.TRANSLATION_X,
                            ViewUtils.convertDpToPx(holder.context, 200))
                    .apply {
                        repeatMode = ObjectAnimator.RESTART
                        repeatCount = ObjectAnimator.INFINITE
                        duration = 2000
                        addListener(CustomAnimatorListener(img, txt, this))
                    }.start()
        }
    }

    override fun getLayoutResId(context: Context): Int = R.layout.item_dashboard_try_swipe

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

private class CustomAnimatorListener(
        private val imageView: ImageView,
        private val textView: TextView,
        private val animator: ObjectAnimator
) : SimpleAnimatorListener() {

    override fun onAnimationStart(animation: Animator) {
        super.onAnimationStart(animation)
        animateFadeIn()
        animateFadeOut()
        textViewAnimateFadeIn()
        textViewAnimateFadeOut()
    }

    override fun onAnimationRepeat(animation: Animator) {
        super.onAnimationRepeat(animation)
        val activity = textView.context.baseActivity
        if (activity == null || activity.isChangingConfigurations || activity.isFinishing) {
            animator.end()
            return
        }

        animateFadeIn()
        animateFadeOut()
        textViewAnimateFadeIn()
        textViewAnimateFadeOut()
    }

    private fun animateFadeIn() {
        ObjectAnimator.ofFloat(imageView, View.ALPHA, 0F, 1F)
                .apply { duration = 800 }.start()
    }

    private fun animateFadeOut() {
        ObjectAnimator.ofFloat(imageView, View.ALPHA, 1F, 0F)
                .apply {
                    duration = 800
                    startDelay = 1200
                }
                .start()
    }

    private fun textViewAnimateFadeIn() {
        ObjectAnimator.ofFloat(textView, View.ALPHA, 0F, 1F)
                .apply {
                    duration = 500
                    startDelay = 800
                }
                .start()
    }

    private fun textViewAnimateFadeOut() {
        ObjectAnimator.ofFloat(textView, View.ALPHA, 1F, 0F)
                .apply {
                    duration = 500
                    startDelay = 1500

                }
                .start()
    }
}
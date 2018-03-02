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
import eu.codetopic.utils.ui.view.getTag
import eu.codetopic.utils.ui.view.setTag
import kotlinx.android.synthetic.main.item_dashboard_try_swipe.view.*
import android.animation.Animator
import android.widget.FrameLayout
import android.widget.ImageView
import eu.codetopic.java.utils.letIfNull
import eu.codetopic.utils.baseActivity
import eu.codetopic.utils.simple.SimpleAnimatorListener
import org.jetbrains.anko.dip
import kotlin.math.min


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

class TrySwipeDashboardItem : SwipeableDashboardItem() {

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

            CustomAnimatorListener.createAnimation(
                    holder.itemView.boxTrySwipe,
                    holder.itemView.imgTrySwipe,
                    holder.itemView.txtTrySwipe.apply { alpha = 0f }
            ).start()
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
        private val box: FrameLayout,
        private val img: ImageView,
        private val txt: TextView,
        private val animator: ObjectAnimator
) : SimpleAnimatorListener() {

    companion object {

        private const val WIDTH_DP_DEFAULT = 200
        private const val DURATION_TOTAL = 2000L
        private const val DURATION_VISIBILITY_IMG = 800L
        private const val DURATION_VISIBILITY_TXT = 250L

        fun createAnimation(box: FrameLayout, img: ImageView, txt: TextView): ObjectAnimator =
                ObjectAnimator.ofFloat(img, View.TRANSLATION_X, 0F).apply {
                    repeatMode = ObjectAnimator.RESTART
                    repeatCount = ObjectAnimator.INFINITE
                    duration = DURATION_TOTAL
                    addListener(CustomAnimatorListener(box, img, txt, this))
                }
    }

    private fun runSelf() {
        val moveX = (box.width - img.width).takeIf { it != 0 }?.toFloat()
                .letIfNull { box.context.dip(WIDTH_DP_DEFAULT).toFloat() }
        animator.setFloatValues(moveX)

        animateFadeIn()
        animateFadeOut()
        textViewAnimateFadeIn()
        textViewAnimateFadeOut()
    }

    override fun onAnimationStart(animation: Animator) {
        super.onAnimationStart(animation)

        runSelf()
    }

    override fun onAnimationRepeat(animation: Animator) {
        super.onAnimationRepeat(animation)
        val activity = txt.context.baseActivity
        if (activity == null || activity.isChangingConfigurations || activity.isFinishing) {
            animator.end()
            return
        }

        runSelf()
    }

    private fun animateFadeIn() {
        ObjectAnimator.ofFloat(img, View.ALPHA, 0F, 1F)
                .apply { duration = DURATION_VISIBILITY_IMG }.start()
    }

    private fun animateFadeOut() {
        ObjectAnimator.ofFloat(img, View.ALPHA, 1F, 0F)
                .apply {
                    duration = DURATION_VISIBILITY_IMG
                    startDelay = DURATION_TOTAL - DURATION_VISIBILITY_IMG
                }
                .start()
    }

    private fun textViewAnimateFadeIn() {
        val sizeDiff = box.width
                .takeIf { it != 0 }?.toFloat()
                ?.let { (it - txt.width - img.width) / it }
                ?: 0.5F

        ObjectAnimator.ofFloat(txt, View.ALPHA, 0F, 1F)
                .apply {
                    duration = DURATION_VISIBILITY_TXT
                    startDelay = min(
                            DURATION_TOTAL - (DURATION_TOTAL * sizeDiff).toLong(),
                            DURATION_TOTAL - (2* DURATION_VISIBILITY_TXT)
                    )

                }
                .start()
    }

    private fun textViewAnimateFadeOut() {
        ObjectAnimator.ofFloat(txt, View.ALPHA, 1F, 0F)
                .apply {
                    duration = DURATION_VISIBILITY_TXT
                    startDelay = DURATION_TOTAL - DURATION_VISIBILITY_TXT

                }
                .start()
    }
}
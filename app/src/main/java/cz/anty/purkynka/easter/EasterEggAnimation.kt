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

package cz.anty.purkynka.easter

import android.animation.Animator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import eu.codetopic.java.utils.to
import eu.codetopic.utils.simple.SimpleAnimatorListener
import eu.codetopic.utils.ui.view.getTag
import eu.codetopic.utils.ui.view.setTag
import kotlin.math.ceil

/**
 * @author anty
 */
class EasterEggAnimation private constructor(private val img: View) {

    companion object {

        private const val LOG_TAG = "EasterEggAnimation"

        private const val VIEW_TAG = "cz.anty.purkynka.easter.$LOG_TAG.INSTANCE"

        private const val FULL_ROTATION = 360F

        private const val STEP_DURATION = 500L
        private const val STEP_DELAY = 0L
        private const val STEP_SCALE_MAX = 1.5F
        private const val STEP_SCALE_FACTOR = 2F

        private const val ROLL_BACK_DURATION = 500L
        private const val ROLL_BACK_DELAY = 200L

        //private const val COUNT_FACTOR = 0.80F
        //private const val COUNT_TARGET = STEP_SCALE_MAX * COUNT_FACTOR
        //private const val COUNT_TO_RUN = 5

        fun applyOn(view: View) = EasterEggAnimation(view)
                .also { view.setTag(VIEW_TAG, it) }.enable()

        //fun resetOn(view: View) = view.getTag(VIEW_TAG).to<EasterEggAnimation>()?.resetView()
    }

    //private var count: Int = 0

    /*private fun countStep() {
        if (img.scaleX >= COUNT_TARGET) count++ else count--
    }*/

    /*private fun resetCount() {
        count = 0
    }*/

    /*private fun checkStep(): Boolean {
        if (count < COUNT_TO_RUN) return false

        val context = img.context
        val options = context.baseActivity?.let {
            ActivityOptionsCompat.makeSceneTransitionAnimation(
                    it, img, context.getString(R.string.id_transition_easter_egg_img)
            )
        }

        if (options == null) Log.e(LOG_TAG, "Can't start EasterEggActivity " +
                "with transition: Cannot find Activity in context hierarchy")

        ContextCompat.startActivity(
                context,
                EasterEggActivity.getStartIntent(context),
                options?.toBundle()
        )

        return true
    }*/

    private fun animateStep() {
        img.animate().cancel()

        val width = img.width
        val height = img.height
        val targetRotation = img.rotation.let { FULL_ROTATION * (ceil(it / FULL_ROTATION) + 1F) }
        val targetScaleX = img.scaleX.let { it + ((STEP_SCALE_MAX - it) / STEP_SCALE_FACTOR) }
        val targetScaleY = img.scaleY.let { it + ((STEP_SCALE_MAX - it) / STEP_SCALE_FACTOR) }
        val targetTranslationX = img.translationX.let { width * (targetScaleX - 1F) / 2F }
        val targetTranslationY = img.translationY.let { height * (targetScaleY - 1F) / 2F }

        img.animate()
                .rotation(targetRotation)
                .scaleX(targetScaleX)
                .scaleY(targetScaleY)
                .translationX(targetTranslationX)
                .translationY(targetTranslationY)
                .setStartDelay(STEP_DELAY)
                .setDuration(STEP_DURATION)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationNaturalEnd(animation: Animator?) {
                        animateRollBack()
                    }
                })
                .start()
    }

    private fun animateRollBack() {
        img.animate().cancel()
        img.animate()
                .rotation(0F)
                .scaleX(1F)
                .scaleY(1F)
                .translationX(0F)
                .translationY(0F)
                .setStartDelay(ROLL_BACK_DELAY)
                .setDuration(ROLL_BACK_DURATION)
                .setInterpolator(AccelerateInterpolator())
                .setListener(object : SimpleAnimatorListener() {
                    override fun onAnimationNaturalEnd(animation: Animator?) {
                        resetView()
                        //resetCount()
                    }
                })
                .start()
    }

    fun resetView() {
        img.animate().cancel()
        img.rotation = 0F
        img.translationX = 0F
        img.translationY = 0f
        img.scaleX = 1F
        img.scaleY = 1F

        //enable()
    }

    fun enable() {
        img.setOnClickListener {
            /*countStep()
            if (!checkStep()) {
                animateStep()
            } else {
                resetCount()
                img.setOnClickListener(null)
            }*/
            animateStep()
        }
    }
}
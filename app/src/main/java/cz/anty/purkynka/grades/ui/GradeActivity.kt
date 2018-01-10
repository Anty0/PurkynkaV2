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

package cz.anty.purkynka.grades.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.Transition
import android.view.View
import android.view.animation.AnimationUtils
import cz.anty.purkynka.R
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.simple.SimpleTransitionListener
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.activity.modular.module.TransitionBackButtonModule
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import kotlinx.android.synthetic.main.activity_grade.*
import kotlinx.serialization.json.JSON

/**
 * @author anty
 */
class GradeActivity : ModularActivity(ToolbarModule(), TransitionBackButtonModule()) {

    companion object {

        private const val LOG_TAG = "GradeActivity"

        private const val EXTRA_GRADE_ITEM =
                "cz.anty.purkynka.grades.ui.$LOG_TAG.EXTRA_GRADE_ITEM"

        fun getStartIntent(context: Context, gradeItem: GradeItem) =
                Intent(context, GradeActivity::class.java)
                        .putExtra(EXTRA_GRADE_ITEM, JSON.stringify(gradeItem))

        fun start(context: Context, gradeItem: GradeItem) =
                context.startActivity(getStartIntent(context, gradeItem))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grade)

        val gradeItem = intent?.getStringExtra(EXTRA_GRADE_ITEM)
                ?.let { JSON.parse<GradeItem>(it) }
                ?:
                run {
                    Log.e(LOG_TAG, "Can't create $LOG_TAG: No GradeItem received")
                    finish()
                    return
                }

        //title = gradeItem.base.? //TODO: maybe set title to something

        val itemVH = gradeItem.createViewHolder(this, boxGrade).also {
            boxGrade.addView(it.itemView, 0)
        }
        gradeItem.bindViewHolder(itemVH, CustomItem.NO_POSITION)

        txtDate.text = gradeItem.base.dateStr
        txtType.text = gradeItem.base.type
        txtSubjectName.text = gradeItem.base.subjectLong
        if (!gradeItem.showSubject) {
            boxSubjectSymbol.visibility = View.VISIBLE
            txtSubjectSymbol.text = gradeItem.base.subjectShort
        } else boxSubjectSymbol.visibility = View.GONE

        if (savedInstanceState == null) {
            val gradeInfoAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            val showGradeInfo = {
                boxGradeInfo.apply {
                    visibility = View.VISIBLE
                    startAnimation(gradeInfoAnimation)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.sharedElementEnterTransition?.apply {
                    addListener(object : SimpleTransitionListener() {
                        override fun onTransitionEnd(transition: Transition) {
                            run(showGradeInfo)
                        }
                    })
                } ?: run(showGradeInfo)
            } else run(showGradeInfo)
        } else {
            boxGradeInfo.visibility = View.VISIBLE
        }
    }
}
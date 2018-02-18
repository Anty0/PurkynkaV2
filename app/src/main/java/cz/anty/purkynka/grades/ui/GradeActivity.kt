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
import cz.anty.purkynka.grades.data.Grade.Companion.dateStr
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.putKSerializableExtra
import eu.codetopic.utils.AndroidExtensions.getKSerializableExtra
import eu.codetopic.utils.simple.SimpleTransitionListener
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.activity.modular.module.TransitionBackButtonModule
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import kotlinx.android.synthetic.main.activity_grade.*
import org.jetbrains.anko.ctx

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
                        .putKSerializableExtra(EXTRA_GRADE_ITEM, gradeItem)

        fun start(context: Context, gradeItem: GradeItem) =
                context.startActivity(getStartIntent(context, gradeItem))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grade)

        val gradeItem = intent?.getKSerializableExtra<GradeItem>(EXTRA_GRADE_ITEM)
                ?:
                run {
                    Log.e(LOG_TAG, "Can't create $LOG_TAG: No GradeItem received")
                    finish()
                    return
                }

        //title = gradeItem.base.? //TODO: maybe set title to something

        val itemVH = gradeItem.createViewHolder(this, boxGrade)
                .also { boxGrade.addView(it.itemView, 0) }
        gradeItem.bindViewHolder(itemVH, CustomItem.NO_POSITION)

        txtDate.text = gradeItem.base.dateStr
        txtType.text = gradeItem.base.type
        txtSubjectName.text = gradeItem.base.subjectLong
        if (!gradeItem.showSubject) {
            boxSubjectSymbol.visibility = View.VISIBLE
            txtSubjectSymbol.text = gradeItem.base.subjectShort
        } else boxSubjectSymbol.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()

        boxGradeInfo.takeIf { it.visibility == View.GONE }
                ?.apply {
                    visibility = View.VISIBLE
                    startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.slide_down))
                }
    }
}
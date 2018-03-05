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
import android.view.View
import android.view.animation.AnimationUtils
import cz.anty.purkynka.R
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Grade.Companion.dateStr
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.getKSerializableExtra
import eu.codetopic.utils.putKSerializableExtra
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.activity.modular.module.TransitionBackButtonModule
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import kotlinx.android.synthetic.main.activity_grade.*
import kotlinx.serialization.internal.NullableSerializer
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.list
import org.jetbrains.anko.ctx

/**
 * @author anty
 */
class GradeActivity : ModularActivity(ToolbarModule(), TransitionBackButtonModule()) {

    companion object {

        private const val LOG_TAG = "GradeActivity"

        private const val EXTRA_GRADE =
                "cz.anty.purkynka.grades.ui.$LOG_TAG.EXTRA_GRADE"
        private const val EXTRA_GRADE_IS_BAD =
                "cz.anty.purkynka.grades.ui.$LOG_TAG.EXTRA_GRADE_IS_BAD"
        private const val EXTRA_GRADE_SHOW_SUBJECT =
                "cz.anty.purkynka.grades.ui.$LOG_TAG.EXTRA_GRADE_SHOW_SUBJECT"
        private const val EXTRA_GRADE_CHANGES =
                "cz.anty.purkynka.grades.ui.$LOG_TAG.EXTRA_GRADE_CHANGES"
        private val EXTRA_GRADE_CHANGES_SERIALIZER = NullableSerializer(StringSerializer.list)

        fun getStartIntent(context: Context, grade: Grade, isBad: Boolean,
                           showSubject: Boolean = true, changes: List<String>? = null) =
                Intent(context, GradeActivity::class.java)
                        .putKSerializableExtra(EXTRA_GRADE, grade)
                        .putExtra(EXTRA_GRADE_IS_BAD, isBad)
                        .putExtra(EXTRA_GRADE_SHOW_SUBJECT, showSubject)
                        .putKSerializableExtra(EXTRA_GRADE_CHANGES, changes,
                                EXTRA_GRADE_CHANGES_SERIALIZER)

        fun start(context: Context, grade: Grade, isBad: Boolean,
                  showSubject: Boolean = true, changes: List<String>? = null) =
                context.startActivity(getStartIntent(context, grade, isBad, showSubject, changes))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grade)

        val grade = intent?.getKSerializableExtra<Grade>(EXTRA_GRADE)
                ?:
                run {
                    Log.e(LOG_TAG, "Can't create $LOG_TAG: No Grade received")
                    finish()
                    return
                }
        val gradeIsBad = intent
                ?.getBooleanExtra(EXTRA_GRADE_IS_BAD, false)
                ?: false
        val gradeShowSubject = intent
                ?.getBooleanExtra(EXTRA_GRADE_SHOW_SUBJECT, true)
                ?: true
        val gradeChanges = intent?.getKSerializableExtra(
                EXTRA_GRADE_CHANGES,
                EXTRA_GRADE_CHANGES_SERIALIZER
        )

        title = getFormattedText(R.string.title_activity_grade_with_subject, grade.subjectShort)

        val gradeItem = GradeItem(grade, gradeIsBad, gradeShowSubject, gradeChanges)
        val itemVH = gradeItem.createViewHolder(this, boxGrade)
                .also { boxGrade.addView(it.itemView, 0) }
        gradeItem.bindViewHolder(itemVH, CustomItem.NO_POSITION)

        txtDate.text = grade.dateStr
        txtType.text = grade.type
        txtSubjectName.text = grade.subjectLong
        if (!gradeShowSubject) {
            boxSubjectSymbol.visibility = View.VISIBLE
            txtSubjectSymbol.text = grade.subjectShort
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

    override fun finishAfterTransition() {
        // fixes bug in Android M (5), that crashes application
        //  if shared element is missing in previous activity.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) super.finishAfterTransition()
        else finish()
    }
}
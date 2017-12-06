/*
 * app
 * Copyright (C)   2017  anty
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
import android.graphics.Color
import android.graphics.Typeface
import android.support.annotation.ColorInt
import android.view.View
import android.widget.TextView
import android.widget.Toast
import cz.anty.purkynka.R
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.Utils.colorForValue
import eu.codetopic.java.utils.JavaExtensions
import eu.codetopic.java.utils.JavaExtensions.join
import eu.codetopic.java.utils.JavaExtensions.fillToLen
import eu.codetopic.utils.ui.container.items.custom.CardViewWrapper
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.items.custom.CustomItemWrapper
import kotlin.math.max
import kotlin.math.min

/**
 * @author anty
 */
class GradeItem(private val base: Grade, private val showSubject: Boolean = true): CustomItem() {

    override fun onBindViewHolder(holder: CustomItem.ViewHolder, itemPosition: Int) {
        val subjectView: TextView = holder.itemView.findViewById(R.id.text_view_subject)
        val gradeView: TextView = holder.itemView.findViewById(R.id.text_view_grade)
        val weightView: TextView = holder.itemView.findViewById(R.id.text_view_weight)
        val noteView: TextView = holder.itemView.findViewById(R.id.text_view_note)
        val teacherView: TextView = holder.itemView.findViewById(R.id.text_view_teacher)

        val textStyle = if (base.weight >= 3) Typeface.BOLD else Typeface.NORMAL

        subjectView.visibility = if (showSubject) View.VISIBLE else View.GONE
        subjectView.text = base.subjectShort.fillToLen(4, JavaExtensions.Anchor.LEFT)

        gradeView.setTypeface(null, textStyle)
        gradeView.setTextColor(colorForValue(base.value.toInt()
                .let { if (it == 0) null else it - 1 }, 5))
        gradeView.text = base.valueToShow

        weightView.setTypeface(null, textStyle)
        weightView.text = base.weight.toString()

        noteView.text = base.note

        teacherView.text = base.teacher

        holder.topParentHolder.itemView.setOnClickListener {
            Toast.makeText(
                    holder.context,
                    "onClick(position$itemPosition, grade=$base)",
                    Toast.LENGTH_LONG
            ).show()
            // TODO: show grade info and stats in new activity
        }
    }

    override fun getItemLayoutResId(context: Context) = R.layout.item_grade

    override fun getWrappers(context: Context?): Array<CustomItemWrapper> = CardViewWrapper.WRAPPER

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GradeItem

        if (base != other.base) return false
        if (showSubject != other.showSubject) return false

        return true
    }

    override fun hashCode(): Int {
        var result = base.hashCode()
        result = 31 * result + showSubject.hashCode()
        return result
    }

    override fun toString(): String {
        return "GradeItem(base=$base, showSubject=$showSubject)"
    }
}
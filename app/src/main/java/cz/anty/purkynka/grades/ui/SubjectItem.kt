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
import android.widget.TextView
import android.widget.Toast
import cz.anty.purkynka.R
import cz.anty.purkynka.Utils.colorForValue
import cz.anty.purkynka.grades.data.Subject
import eu.codetopic.java.utils.JavaExtensions
import eu.codetopic.java.utils.JavaExtensions.fillToLen
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.java.utils.JavaExtensions.format
import eu.codetopic.utils.notifications.manager.data.NotificationId
import kotlinx.android.synthetic.main.item_subject.*

/**
 * @author anty
 */
class SubjectItem(val base: Subject,
                  val changes: Map<Int, List<String>> = emptyMap()): CustomItem() { // TODO: use changes

    val isChnaged get() = changes.isNotEmpty()

    override fun onBindViewHolder(holder: ViewHolder, itemPosition: Int) {
        holder.txtNameShort.text = base.shortName.fillToLen(4, JavaExtensions.Anchor.LEFT)

        holder.txtAverage.apply {
            val average = base.average
            setTextColor(colorForValue((average * 100).toInt()
                    .let { if (it == 0) null else it - 100 }, 500))
            text = average.format(2)
        }


        holder.txtNameLong.text = base.fullName

        val gradesCount = base.grades.size
        holder.txtGradesCount.text = holder.context.resources
                .getQuantityString(R.plurals.text_view_grades_count, gradesCount)
                .format(gradesCount)

        holder.boxColoredBackground.setBackgroundResource(
                if (!isChnaged) android.R.color.transparent
                else R.color.colorPrimaryExtraDarkGrades
        )

        holder.boxClickTarget.setOnClickListener {
            Toast.makeText(
                    holder.context,
                    "onClick(position$itemPosition, subject=$this)",
                    Toast.LENGTH_LONG
            ).show()
            // TODO: show activity of subject grades
        }
    }

    override fun getItemLayoutResId(context: Context): Int = R.layout.item_subject

    //override fun getWrappers(context: Context): Array<CustomItemWrapper> = CardViewWrapper.WRAPPER

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubjectItem

        if (base != other.base) return false

        return true
    }

    override fun hashCode(): Int = base.hashCode()

    override fun toString(): String = "SubjectItem(base=$base)"
}
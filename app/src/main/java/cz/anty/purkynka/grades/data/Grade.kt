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

package cz.anty.purkynka.grades.data

import android.content.Context
import cz.anty.purkynka.grades.load.GradesParser
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import java.util.*

/**
 * @author anty
 */
data class Grade(val date: Date, val shortLesson: String, val longLesson: String, val valueToShow: String,
                 val value: Double, val type: String, val weight: Int, val note: String, val teacher: String): CustomItem() {

    val dateStr: String get() = GradesParser.GRADE_DATE_FORMAT.format(date)

    override fun onBindViewHolder(holder: CustomItem.ViewHolder?, itemPosition: Int) {
        TODO("not implemented")
    }

    override fun getItemLayoutResId(context: Context?): Int {
        TODO("not implemented")
    }
}
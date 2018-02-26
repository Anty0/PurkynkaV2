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
import cz.anty.purkynka.R
import cz.anty.purkynka.grades.util.GradesSort
import cz.anty.purkynka.grades.util.GradesSort.*
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.android.synthetic.main.item_grades_sort_spinner.*
import org.jetbrains.anko.textResource

/**
 * @author anty
 */
class GradesSortSpinnerItem(val sort: GradesSort) : CustomItem() {

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {
        holder.txtSortName.textResource = when (sort) {
            GRADES_DATE -> R.string.menu_item_text_show_grades_date
            GRADES_VALUE -> R.string.menu_item_text_show_grades_value
            GRADES_SUBJECT -> R.string.menu_item_text_show_grades_subject
            SUBJECTS_NAME -> R.string.menu_item_text_show_subjects_name
            SUBJECTS_AVERAGE_BEST -> R.string.menu_item_text_show_subjects_average_best
            SUBJECTS_AVERAGE_WORSE -> R.string.menu_item_text_show_subjects_average_worse
        }
    }

    override fun getLayoutResId(context: Context): Int = R.layout.item_grades_sort_spinner

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GradesSortSpinnerItem

        if (sort != other.sort) return false

        return true
    }

    override fun hashCode(): Int {
        return sort.hashCode()
    }
}
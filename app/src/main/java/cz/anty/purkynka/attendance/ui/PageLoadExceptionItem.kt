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

package cz.anty.purkynka.attendance.ui

import android.content.Context
import cz.anty.purkynka.R
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import kotlinx.android.synthetic.main.item_attendance_exception_page_load.*

/**
 * @author anty
 */
class PageLoadExceptionItem(val ex: Exception) : CustomItem() {

    override fun onBindViewHolder(holder: ViewHolder, itemPosition: Int) {
        holder.txtExceptionType.text = ex.toString()
    }

    override fun getItemLayoutResId(context: Context): Int =
            R.layout.item_attendance_exception_page_load
}
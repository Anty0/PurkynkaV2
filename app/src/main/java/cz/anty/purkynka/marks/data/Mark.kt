/*
 * ApplicationPurkynka
 * Copyright (C)  2017  anty
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cz.anty.purkynka.marks.data

import android.content.Context
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by anty on 6/20/17.
 * @author anty
 */
data class Mark(val date: Date, val shortLesson: String, val longLesson: String, val valueToShow: String,
                val value: Double, val type: String, val weight: Int, val note: String, val teacher: String): CustomItem() {

    override fun onBindViewHolder(holder: CustomItem.ViewHolder?, itemPosition: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemLayoutResId(context: Context?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
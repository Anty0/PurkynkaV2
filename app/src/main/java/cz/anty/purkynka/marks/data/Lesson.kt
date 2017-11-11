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
import com.google.common.collect.ImmutableList
import eu.codetopic.utils.ui.container.items.custom.CustomItem

/**
 * Created by anty on 6/20/17.
 * @author anty
 */
data class Lesson(val fullName: String, val shortName: String, val marks: ImmutableList<Mark>): CustomItem() {

    fun getDiameter(): Double {
        var tempMark = 0.0
        var tempWeight = 0
        for (mark in marks) {
            if (mark.value == 0.0) continue
            tempMark += mark.value * mark.weight.toDouble()
            tempWeight += mark.weight
        }
        return tempMark / tempWeight.toDouble()
    }

    override fun onBindViewHolder(holder: ViewHolder?, itemPosition: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemLayoutResId(context: Context?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
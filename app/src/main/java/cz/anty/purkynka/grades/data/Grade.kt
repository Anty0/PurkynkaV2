/*
 * Copyright 2017 Jiří Kuchyňka (Anty)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
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
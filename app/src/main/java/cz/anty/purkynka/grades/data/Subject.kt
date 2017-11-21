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
import eu.codetopic.utils.ui.container.items.custom.CustomItem

/**
 * @author anty
 */
data class Subject(val fullName: String, val shortName: String, val grades: List<Grade>): CustomItem() {

    val diameter: Double get() {
        var tGrade = 0.0
        var tWeight = 0
        return grades.filterNot { it.value == 0.0 }
                .takeIf { it.isNotEmpty() }
                ?.onEach {
                    tGrade += it.value * it.weight.toDouble()
                    tWeight += it.weight
                }
                ?.let {
                    tGrade / tWeight.toDouble()
                } ?: Double.NaN
    }

    override fun onBindViewHolder(holder: ViewHolder?, itemPosition: Int) {
        TODO("not implemented")
    }

    override fun getItemLayoutResId(context: Context?): Int {
        TODO("not implemented")
    }

}
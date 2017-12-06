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
import android.widget.TextView
import android.widget.Toast
import cz.anty.purkynka.R
import eu.codetopic.utils.ui.container.items.custom.CardViewWrapper
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.items.custom.CustomItemWrapper

/**
 * @author anty
 */
data class Subject(val fullName: String, val shortName: String, val grades: List<Grade>) {

    val average: Double get() {
        var tGrade = 0.0
        var tWeight = 0
        return grades.filterNot { it.value == 0F }
                .takeIf { it.isNotEmpty() }
                ?.onEach {
                    tGrade += it.value * it.weight.toDouble()
                    tWeight += it.weight
                }
                ?.let {
                    tGrade / tWeight.toDouble()
                } ?: Double.NaN
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Subject

        if (fullName != other.fullName) return false
        if (shortName != other.shortName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fullName.hashCode()
        result = 31 * result + shortName.hashCode()
        return result
    }
}
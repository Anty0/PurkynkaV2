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

import android.support.annotation.ColorInt
import cz.anty.purkynka.utils.*
import kotlinx.serialization.Serializable

/**
 * @author anty
 */
@Serializable
data class Subject(val fullName: String, val shortName: String, val grades: List<Grade>) {

    companion object {

        val Collection<Grade>.average: Double
            get() {
                var tValue = 0.0
                var tWeight = 0
                return this.filterNot { it.value == 0F }
                        .takeIf { it.isNotEmpty() }
                        ?.onEach {
                            val weight = it.weight
                            tValue += it.value * weight.toDouble()
                            tWeight += weight
                        }
                        ?.let {
                            tValue / tWeight.toDouble()
                        } ?: Double.NaN
            }

        val Subject.average: Double
            get() = grades.average

        @get:ColorInt
        val Collection<Grade>.averageColor: Int
            get() = colorForValue(
                    value = (average * 100).toInt() - 100,
                    size = 500
            )

        @get:ColorInt
        val Subject.averageColor: Int
            get() = grades.averageColor
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
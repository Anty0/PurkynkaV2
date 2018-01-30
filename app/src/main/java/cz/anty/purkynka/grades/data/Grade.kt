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
import cz.anty.purkynka.Utils
import cz.anty.purkynka.grades.load.GradesParser
import eu.codetopic.java.utils.JavaExtensions.letIf
import kotlinx.serialization.Serializable
import java.util.*

/**
 * @author anty
 */
@Serializable
data class Grade(val id: Int, val date: Long, val subjectShort: String, val subjectLong: String, val valueToShow: String,
                 val value: Float, val type: String, val weight: Int, val note: String, val teacher: String) {

    companion object {

        val Grade.dateStr: String
            get() = GradesParser.GRADE_DATE_FORMAT.format(Date(date))

        @get:ColorInt
        val Grade.valueColor: Int
            get() = Utils.colorForValue(
                    value = value.toInt()
                            .letIf({ it == 0 }) { null }
                            ?.let { it - 1 },
                    size = 5
            )
    }

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Grade

        if (id != other.id) return false

        return true
    }

    infix fun differentTo(other: Grade) = id != other.id
            || date != other.date
            || subjectShort != other.subjectShort
            || subjectLong != other.subjectLong
            || valueToShow != other.valueToShow
            || value != other.value
            || type != other.type
            || weight != other.weight
            || note != other.note
            || teacher != other.teacher
}
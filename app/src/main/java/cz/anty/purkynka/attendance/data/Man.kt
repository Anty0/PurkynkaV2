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

package cz.anty.purkynka.attendance.data

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author anty
 */
@Serializable
data class Man(val name: String, val classId: String,
               val lastEnterTime: Long, val isInSchool: Boolean?) {

    companion object {

        private val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH)

        val Man.lastEnterStr: String
            get() = DATE_FORMAT.format(lastEnterTime)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Man

        if (name != other.name) return false
        if (classId != other.classId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + classId.hashCode()
        return result
    }
}
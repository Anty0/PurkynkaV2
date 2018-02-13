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

package cz.anty.purkynka.lunches.data

import cz.anty.purkynka.lunches.load.LunchesParser
import kotlinx.serialization.Serializable

/**
 * @author anty
 */
@Serializable
data class LunchBurza(val lunchNumber: Int, val date: Long, val name: String,
                      val canteen: String, val pieces: Int, val orderUrl: String) {

    companion object {

        val LunchBurza.dateStr: String
            get() = LunchesParser.FORMAT_DATE_SHOW.format(date)

        val LunchBurza.dateStrShort: String
            get() = LunchesParser.FORMAT_DATE_SHOW_SHORT.format(date)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LunchBurza

        if (date != other.date) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}
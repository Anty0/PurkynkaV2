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
data class LunchOption(val name: String, val date: Long, val enabled: Boolean, val ordered: Boolean,
                       val orderOrCancelUrl: String?, val isInBurza: Boolean?, val toOrFromBurzaUrl: String?) {

    companion object {

        val LunchOption.dateStr: String
            get() = LunchesParser.FORMAT_DATE_SHOW.format(date)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + date.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LunchOption

        if (name != other.name) return false
        if (date != other.date) return false

        return true
    }
}
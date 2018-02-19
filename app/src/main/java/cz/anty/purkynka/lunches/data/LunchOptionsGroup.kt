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
import kotlinx.serialization.Transient
import java.util.*

/**
 * @author anty
 */
@Serializable
data class LunchOptionsGroup(val date: Long, val options: Array<LunchOption>?) {

    companion object {

        val LunchOptionsGroup.dateStr: String
            get() = LunchesParser.FORMAT_DATE_SHOW.format(date)

        val LunchOptionsGroup.dateStrShort: String
            get() = LunchesParser.FORMAT_DATE_SHOW_SHORT.format(date)
    }

    @Transient
    val orderedOption: Pair<Int, LunchOption>?
        get() = options?.indexOfFirst { it.ordered }
                ?.takeIf { it != -1 }
                ?.let { it to options[it] }

    infix fun isDifferentFrom(other: LunchOptionsGroup): Boolean {
        return date != other.date ||
                run checkOptions@ {
            if (options == null) return other.options != null
            if (other.options == null) return true

            if (options.size != other.options.size) return true

            return (0 until options.size).any {
                options[it] isDifferentFrom other.options[it]
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LunchOptionsGroup

        if (date != other.date) return false
        if (!Arrays.equals(options, other.options)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + Arrays.hashCode(options)
        return result
    }
}
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

package cz.anty.purkynka.grades.data

import java.util.Calendar
import java.util.Date

/**
 * @author anty
 */
enum class Semester {
    FIRST, SECOND, AUTO;

    val stableSemester: Semester
        get() {
            return when (value) {
                2 -> SECOND
                1 -> FIRST
                else -> FIRST
            }
        }

    val value: Int
        get() {
            return when (this) {
                FIRST -> 1
                SECOND -> 2
                AUTO -> {
                    Calendar.getInstance()
                            .apply { time = Date(System.currentTimeMillis()) }
                            .get(Calendar.MONTH)
                            .let {
                                if (it > Calendar.JANUARY && it < Calendar.JULY)
                                    2 else 1
                            }
                }
            }
        }

    fun reverse(): Semester {
        return when (value) {
            2 -> FIRST
            1 -> SECOND
            else -> SECOND
        }
    }
}

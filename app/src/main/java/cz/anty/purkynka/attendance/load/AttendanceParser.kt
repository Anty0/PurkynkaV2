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

package cz.anty.purkynka.attendance.load

import cz.anty.purkynka.attendance.data.Man
import eu.codetopic.java.utils.log.Log
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


/**
 * @author anty
 */
object AttendanceParser {

    private const val LOG_TAG = "AttendanceParser"

    private const val COL_NAME = 0
    private const val COL_CLASS_ID = 1
    private const val COL_LAST_ENTER_DATE = 2
    private const val COL_IS_IN_SCHOOL = 3

    private val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH)

    fun parseMans(elementMans: Elements): List<Man> =
            elementMans.mapNotNull {
                try {
                    return@mapNotNull parseMan(it)
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "parseMans", e)
                    return@mapNotNull null
                }
            }

    fun parseMan(man: Element): Man {
        val manData = man.select("td")

        val name = manData[COL_NAME].text()
        val classId = manData[COL_CLASS_ID].text()

        val lastEnterTime = try {
            DATE_FORMAT.parse(manData[COL_LAST_ENTER_DATE].text()).time
        } catch (e: ParseException) {
            throw IllegalArgumentException(
                    "Parameter error: invalid " +
                            "last enter date ${manData[COL_LAST_ENTER_DATE].text()}",
                    e
            )
        }
        val isInSchool = parseIsInSchool(manData[COL_IS_IN_SCHOOL])

            return Man(name, classId, lastEnterTime, isInSchool)
    }

    fun parseIsInSchool(isInSchoolElement: Element): Boolean? {
        val toParse = isInSchoolElement.text().toLowerCase(Locale.getDefault())
        return when {
            "nezjištěn" == toParse -> null
            "ne" in toParse -> false
            else -> true
        }
    }
}
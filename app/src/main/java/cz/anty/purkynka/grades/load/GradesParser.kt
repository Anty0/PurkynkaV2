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

package cz.anty.purkynka.grades.load

import android.content.SyncResult
import android.net.Uri
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Subject
import eu.codetopic.java.utils.log.Log
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by anty on 10/9/17.
 * @author anty
 */
object GradesParser {

    private const val LOG_TAG = "GradesParser"

    private const val COL_DATE = 0
    private const val COL_SUBJECT = 1
    private const val COL_VALUE_STR = 2
    private const val COL_VALUE_FLOAT = 3
    private const val COL_TYPE = 4
    private const val COL_WEIGHT = 5
    private const val COL_NOTE = 6
    private const val COL_TEACHER = 7

    private val GRADE_DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH)

    fun parseSubjects(elementGrades: Elements): List<Subject> {
        return parseGrades(elementGrades).toSubjects()
    }

    fun List<Grade>.toSubjects(): List<Subject> = mutableMapOf<String, MutableList<Grade>>()
            .apply {
                this@toSubjects.forEach {
                    getOrPut(it.subjectShort, ::mutableListOf).add(it)
                }
            }
            .map {
                Subject(it.value.first().subjectLong,
                        it.value.first().subjectShort,
                        it.value.toList())
            }
            .sortedBy { it.shortName }

    fun parseGrades(gradesHtml: Elements, syncResult: SyncResult? = null): List<Grade> = gradesHtml
            .takeIf { it.isNotEmpty() }
            ?.takeIf {
                it[0].select("td").let {
                    it.isNotEmpty() && !it[0].text().contains("žádné", true)
                }
            }
            ?.mapNotNull {
                try {
                    syncResult?.apply { stats.numEntries++ }
                    parseGrade(it)
                } catch (e: Exception) {
                    syncResult?.apply { stats.numParseExceptions++ }
                    Log.w(LOG_TAG, "parseGrades", e); null
                }
            }
            ?: emptyList()

    fun parseGrade(grade: Element): Grade {
        val gradeData = grade.select("td")

        val dateElement = gradeData[COL_DATE].child(0)
        val id = Uri.parse(dateElement.attr("abs:href"))
                .getQueryParameter("zaznam").toInt()
        val date = try {
            GRADE_DATE_FORMAT.parse(dateElement.text()).time
        } catch (e: ParseException) {
            throw IllegalArgumentException(
                    "Parameter error: invalid date ${gradeData[0].text()}", e
            )
        }

        val subjectElement = gradeData[COL_SUBJECT]
        val subjectShort = subjectElement.text()
        val subjectLong = subjectElement.attr("title")

        val valueStr = gradeData[COL_VALUE_STR].text()

        val valueFloat = gradeData[COL_VALUE_FLOAT].text().toFloat()

        val type = gradeData[COL_TYPE].text()

        val weight = gradeData[COL_WEIGHT].text().toInt()

        val note = gradeData[COL_NOTE].text()

        val teacher = gradeData[COL_TEACHER].text()

        return Grade(id, date, subjectShort, subjectLong,
                valueStr, valueFloat, type, weight, note, teacher)
    }

}
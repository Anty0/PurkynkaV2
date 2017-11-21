/*
 * Copyright 2017 Jiří Kuchyňka (Anty)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package cz.anty.purkynka.grades.load

import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Subject
import eu.codetopic.java.utils.log.Log
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


/**
 * Created by anty on 10/9/17.
 * @author anty
 */
object GradesParser {

    private const val LOG_TAG = "GradesParser"

    val GRADE_DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun toSubjects(elementGrades: Elements): List<Subject> {
        return parseGrades(elementGrades).toSubjects()
    }

    fun List<Grade>.toSubjects(): List<Subject> {
        return mutableMapOf<String, MutableList<Grade>>().apply {
            this@toSubjects.forEach {
                getOrPut(it.shortLesson, ::mutableListOf).add(it)
            }
        }.map {
            Subject(it.value.first().longLesson,
                    it.value.first().shortLesson,
                    it.value.toList())
        }.sortedWith(Comparator { lhs, rhs -> lhs.shortName.compareTo(rhs.shortName) })


        /*val subjectsMap = HashMap<String, MutableList<Grade>>()
        for (grade in this) {
            if (!subjectsMap.containsKey(grade.shortLesson)) {
                subjectsMap.put(grade.shortLesson, mutableListOf())
            }
            subjectsMap[grade.shortLesson]!!.add(grade)
        }

        val subjects = mutableListOf<Subject>()
        subjectsMap.forEach {
            subjects.add(Subject(it.value.first().longLesson,
                    it.value.first().shortLesson, it.value.toList()))
        }
        Collections.sort(subjects) { lhs, rhs -> lhs.shortName.compareTo(rhs.shortName) }
        return subjects*/
    }

    fun parseGrades(elementGrades: Elements): List<Grade> {
        val grades = ArrayList<Grade>()

        if (!elementGrades.isEmpty()) {
            val firstGrade = elementGrades[0].select("td")
            if (firstGrade.isEmpty() || firstGrade[0].text().toLowerCase().contains("žádné"))
                return grades
        }

        for (grade in elementGrades) {
            try {
                grades.add(parseGrade(grade))
            } catch (e: IllegalArgumentException) {
                Log.d(LOG_TAG, "parseGrades", e)
            }

        }
        return grades
    }

    fun parseGrade(grade: Element): Grade {
        val gradeData = grade.select("td")

        try {
            return Grade(GRADE_DATE_FORMAT.parse(gradeData[0].text()),
                    gradeData[1].text(), gradeData[1].attr("title"), gradeData[2].text(),
                    gradeData[3].text().toDouble(), gradeData[4].text(),
                    gradeData[5].text().toInt(), gradeData[6].text(), gradeData[7].text())
        } catch (e: ParseException) {
            throw IllegalArgumentException("Parameter error: invalid date " + gradeData[0].text(), e)
        }
    }

}
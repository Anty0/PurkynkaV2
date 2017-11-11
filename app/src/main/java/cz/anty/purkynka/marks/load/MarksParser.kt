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

package cz.anty.purkynka.marks.load

import cz.anty.purkynka.marks.data.Mark
import com.google.common.collect.ImmutableList
import cz.anty.purkynka.marks.data.Lesson
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
object MarksParser {

    val MARK_DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun toLessons(elementMarks: Elements): List<Lesson> {
        return parseMarks(elementMarks).toLessons()
    }

    fun List<Mark>.toLessons(): List<Lesson> {
        val lessonsMap = HashMap<Array<String>, ArrayList<Mark>>()
        for (mark in this) {
            val key = arrayOf(mark.longLesson, mark.shortLesson)
            if (!lessonsMap.containsKey(key)) {
                lessonsMap.put(key, ArrayList())
            }
            lessonsMap[key]!!.add(mark)
        }

        val lessons = ArrayList<Lesson>()
        lessonsMap.forEach { entry ->
            lessons.add(Lesson(entry.key[0], entry.key[1], ImmutableList.copyOf(entry.value)))
        }
        Collections.sort(lessons) { lhs, rhs -> lhs.shortName.compareTo(rhs.shortName) }
        return lessons
    }

    fun parseMarks(elementMarks: Elements): List<Mark> {
        val marks = ArrayList<Mark>()

        if (!elementMarks.isEmpty()) {
            val firstMark = elementMarks[0].select("td")
            if (firstMark.isEmpty() || firstMark[0].text().toLowerCase().contains("žádné"))
                return marks
        }

        for (mark in elementMarks) {
            try {
                marks.add(parseMark(mark))
            } catch (e: IllegalArgumentException) {
                Log.d("Marks", "parseMarks", e)
            }

        }
        return marks
    }

    fun parseMark(mark: Element): Mark {
        val markData = mark.select("td")

        try {
            return Mark(MARK_DATE_FORMAT.parse(markData[0].text()),
                    markData[1].text(), markData[1].attr("title"), markData[2].text(),
                    markData[3].text().toDouble(), markData[4].text(),
                    markData[5].text().toInt(), markData[6].text(), markData[7].text())
        } catch (e: ParseException) {
            throw IllegalArgumentException("Parameter error: invalid date " + markData[0].text(), e)
        }
    }

}
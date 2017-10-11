package cz.anty.purkynka.marks

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
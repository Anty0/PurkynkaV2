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

package cz.anty.purkynka.grades.notify

import android.app.NotificationChannelGroup
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.MainActivity

import cz.anty.purkynka.R
import cz.anty.purkynka.grades.GradesFragment
import cz.anty.purkynka.grades.data.Grade
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.getFormattedText
import eu.codetopic.utils.ids.Identifiers
import eu.codetopic.utils.notifications.manager.data.NotificationId
import eu.codetopic.utils.notifications.manager.util.NotificationChannel
import eu.codetopic.utils.ids.Identifiers.Companion.nextId
import eu.codetopic.utils.notifications.manager.util.SummarizedNotificationGroup
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import kotlin.coroutines.experimental.buildSequence

/**
 * @author anty
 */
class GradesChangesNotificationGroup :
        SummarizedNotificationGroup(ID, true) {

    companion object {

        private const val LOG_TAG = "GradesChangesNotificationGroup"
        const val ID = "GRADES_CHANGES"

        private val idType = Identifiers.Type(ID)

        private const val PARAM_TYPE = "TYPE"
        private const val PARAM_TYPE_VAL_NEW = "NEW"
        private const val PARAM_TYPE_VAL_MODIFIED = "MODIFIED"
        private const val PARAM_GRADE = "GRADE"
        private const val PARAM_CHANGES_LIST = "CHANGES"

        fun dataForNewGrade(grade: Grade): Bundle = Bundle().apply {
            putString(PARAM_TYPE, PARAM_TYPE_VAL_NEW)
            putString(PARAM_GRADE, JSON.stringify(grade))
        }

        fun dataForModifiedGrade(oldGrade: Grade, newGrade: Grade): Bundle = Bundle().apply {
            putString(PARAM_TYPE, PARAM_TYPE_VAL_MODIFIED)
            putString(PARAM_GRADE, JSON.stringify(newGrade))
            putString(
                    PARAM_CHANGES_LIST,
                    JSON.stringify(
                            StringSerializer.list,
                            buildSequence {
                                if (oldGrade.date != newGrade.date) yield("date")
                                if (oldGrade.subjectShort != newGrade.subjectShort) yield("subjectShort")
                                if (oldGrade.subjectLong != newGrade.subjectLong) yield("subjectLong")
                                if (oldGrade.valueToShow != newGrade.valueToShow) yield("valueToShow")
                                if (oldGrade.value != newGrade.value) yield("value")
                                if (oldGrade.type != newGrade.type) yield("type")
                                if (oldGrade.weight != newGrade.weight) yield("weight")
                                if (oldGrade.note != newGrade.note) yield("note")
                                if (oldGrade.teacher != newGrade.teacher) yield("teacher")
                            }.toList()
                    )
            )
        }

        fun readDataGrade(data: Bundle): Grade? =
                data.getString(PARAM_GRADE)?.let { JSON.parse(it) }


        fun readDataChanges(data: Bundle): List<String>? =
                when (data.getString(PARAM_TYPE)) {
                    PARAM_TYPE_VAL_NEW -> emptyList()
                    PARAM_TYPE_VAL_MODIFIED -> data.getString(PARAM_CHANGES_LIST)
                            ?.let { JSON.parse(StringSerializer.list, it) }
                            ?: emptyList()
                    else -> null
                }
    }

    override fun nextId(context: Context, channel: NotificationChannel,
                        data: Bundle): Int = idType.nextId()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createGroup(context: Context): NotificationChannelGroup =
            NotificationChannelGroup(id, context.getText(R.string.notify_group_name_grades_changes))

    override fun handleContentIntent(context: Context, id: NotificationId,
                                     channel: NotificationChannel, data: Bundle) {
        // TODO: show grade info activity rather then all grades fragment
        // (use context.startActivities() to start main activity with grade info activity on top of it]
        MainActivity.start(context, GradesFragment::class.java)
    }

    override fun handleSummaryContentIntent(context: Context, id: NotificationId,
                                            channel: NotificationChannel, data: List<Bundle>) {
        MainActivity.start(context, GradesFragment::class.java)
    }

    private fun buildNotificationBase(context: Context,
                                      channel: NotificationChannel): NotificationCompat.Builder =
            NotificationCompat.Builder(context, channel.id).apply {
                //setContentTitle(context.getFormattedText(R.string.notify_grade_new_title,
                //            grade.valueToShow, grade.subjectShort))
                //setContentText(context.getFormattedText(R.string.notify_grade_new_text, grade.teacher))
                //setSubText()
                //setTicker()
                //setUsesChronometer()
                //setNumber()
                /*setWhen(grade.date.time)*/
                //setShowWhen(true)
                //setStyle()

                setSmallIcon(R.mipmap.ic_launcher_grades)
                //setLargeIcon()
                color = ContextCompat.getColor(context, R.color.colorPrimaryGrades)
                //setColorized(false)

                setDefaults(NotificationCompat.DEFAULT_ALL)

                setAutoCancel(true)
                setCategory(NotificationCompat.CATEGORY_EVENT)

                setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                //setTimeoutAfter()
                //setOngoing()
                //setPublicVersion()

                //addAction()
                extend(NotificationCompat.WearableExtender()
                        .setHintContentIntentLaunchesActivity(true))
            }

    override fun createNotification(context: Context,
                                    id: NotificationId,
                                    channel: NotificationChannel,
                                    data: Bundle): NotificationCompat.Builder =
            buildNotificationBase(context, channel).apply {
                val grade = data.getString(PARAM_GRADE)?.let { JSON.parse<Grade>(it) }
                        ?: throw IllegalArgumentException("Data doesn't contains grade")
                when (data.getString(PARAM_TYPE)) {
                    PARAM_TYPE_VAL_NEW -> {
                        setContentTitle(context.getFormattedText(
                                R.string.notify_grade_new_title,
                                grade.valueToShow, grade.subjectShort
                        ))
                        setContentText(context.getFormattedText(
                                if (grade.note.isEmpty()) R.string.notify_grade_new_text
                                else R.string.notify_grade_new_text_note,
                                grade.note, grade.teacher
                        ))
                    }
                    PARAM_TYPE_VAL_MODIFIED -> {
                        val changes = data.getString(PARAM_CHANGES_LIST)
                                ?.let { JSON.parse(StringSerializer.list, it) }
                                ?: throw IllegalArgumentException(
                                "Data doesn't contains grade's changes list")


                        // TODO: show changes
                        setContentTitle(context.getFormattedText(
                                R.string.notify_grade_modified_title,
                                grade.valueToShow, grade.subjectShort
                        ))
                        setContentText(context.getFormattedText(
                                if (grade.note.isEmpty()) R.string.notify_grade_modified_text
                                else R.string.notify_grade_modified_text_note,
                                grade.note, grade.teacher
                        ))
                    }
                    else -> throw IllegalArgumentException(
                            "Data doesn't contains valid change type")
                }
            }

    override fun createSummaryNotification(context: Context,
                                           id: NotificationId,
                                           channel: NotificationChannel,
                                           data: List<Bundle>): NotificationCompat.Builder {
        val allGrades = data.mapNotNull {
            it.getString(PARAM_GRADE)?.let { JSON.parse<Grade>(it) }.also {
                if (it == null) Log.w(LOG_TAG, "Data doesn't contains grade")
            }
        }
        val subjects = allGrades.map { it.subjectShort }.distinct().joinToString(", ")

        return buildNotificationBase(context, channel).apply {
            setContentTitle(context.getText(R.string.notify_grade_summary_title))
            setContentText(context.getFormattedText(R.string.notify_grade_summary_text, subjects))
            setStyle(NotificationCompat.InboxStyle()
                    .setBigContentTitle(context.getText(R.string.notify_grade_summary_title))
                    .setSummaryText(context.getFormattedText(R.string.notify_grade_summary_text, subjects))
                    .also { n ->
                        allGrades.forEach {
                            n.addLine(context.getFormattedText(R.string.notify_grade_summary_line,
                                    it.valueToShow, it.subjectShort))
                        }
                    })
        }
    }
}
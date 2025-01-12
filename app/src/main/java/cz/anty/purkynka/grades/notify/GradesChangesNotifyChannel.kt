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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.MainActivity
import cz.anty.purkynka.R
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.account.save.ActiveAccount
import cz.anty.purkynka.grades.GradesFragment
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Grade.Companion.valueColor
import cz.anty.purkynka.grades.data.Subject.Companion.averageColor
import cz.anty.purkynka.grades.ui.GradeActivity
import eu.codetopic.java.utils.alsoIfNull
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.ids.Identifiers
import eu.codetopic.utils.ids.Identifiers.Companion.nextId
import eu.codetopic.utils.notifications.manager.combinedIdFor
import eu.codetopic.utils.notifications.manager.data.NotifyId
import eu.codetopic.utils.notifications.manager.util.NotifyGroup
import eu.codetopic.utils.notifications.manager.util.SummarizedNotifyChannel
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import kotlin.coroutines.experimental.buildSequence

/**
 * @author anty
 */
class GradesChangesNotifyChannel : SummarizedNotifyChannel(ID, checkForIdOverrides = true) {

    companion object {

        private const val LOG_TAG = "GradesChangesNotifyChannel"
        const val ID = "${BuildConfig.APPLICATION_ID}.grades.notify.changes"

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

    override fun nextId(context: Context, group: NotifyGroup,
                        data: Bundle): Int = idType.nextId()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel(context: Context, combinedId: String): NotificationChannel =
            NotificationChannel(
                    combinedId,
                    context.getText(R.string.notify_channel_name_grades_changes),
                    NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                setBypassDnd(false)
                setShowBadge(true)
                this.lightColor = ContextCompat.getColor(context, R.color.colorPrimaryGrades)
            }

    override fun handleContentIntent(context: Context, group: NotifyGroup,
                                     notifyId: NotifyId, data: Bundle) {
        val accountId = (group as? AccountNotifyGroup)?.accountId.alsoIfNull {
            Log.e(LOG_TAG, "handleContentIntent(id=$notifyId, group=$group," +
                    " notifyId=$notifyId, data=$data)",
                    IllegalArgumentException("Group is not AccountNotifyGroup, " +
                            "can't change ActiveAccount to correct account."))
        }
        val grade = readDataGrade(data).alsoIfNull {
            Log.e(LOG_TAG, "handleContentIntent(id=$notifyId, group=$group," +
                    " notifyId=$notifyId, data=$data)",
                    IllegalArgumentException("Data doesn't contains grade"))
        }
        val changes = readDataChanges(data).alsoIfNull {
            Log.e(LOG_TAG, "handleContentIntent(id=$notifyId, group=$group," +
                    " notifyId=$notifyId, data=$data)",
                    IllegalArgumentException("Failed to read grade changes"))
        }

        accountId?.let { ActiveAccount.set(it) }

        if (grade != null) {
            context.startActivities(arrayOf(
                    MainActivity.getStartIntent(context, GradesFragment::class.java)
                            .addFlags(FLAG_ACTIVITY_NEW_TASK),
                    GradeActivity.getStartIntent(context, grade, false, changes = changes)
            ))
        } else {
            context.startActivity(
                    MainActivity.getStartIntent(context, GradesFragment::class.java)
                            .addFlags(FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    override fun handleSummaryContentIntent(context: Context, group: NotifyGroup,
                                            notifyId: NotifyId, data: Map<out NotifyId, Bundle>) {
        val accountId = (group as? AccountNotifyGroup)?.accountId.alsoIfNull {
            Log.e(LOG_TAG, "handleSummaryContentIntent(id=$notifyId, group=$group," +
                    " notifyId=$notifyId, data=$data)",
                    IllegalArgumentException("Group is not AccountNotifyGroup, " +
                            "can't change ActiveAccount to correct account."))
        }

        accountId?.let { ActiveAccount.set(it) }

        context.startActivity(
                MainActivity.getStartIntent(context, GradesFragment::class.java)
                        .addFlags(FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun buildNotificationBase(context: Context, group: NotifyGroup,
                                      notifyColor: Int): NotificationCompat.Builder =
            NotificationCompat.Builder(context, combinedIdFor(group)).apply {
                //setContentTitle(context.getFormattedText(R.string.notify_grade_new_title,
                //            grade.valueToShow, grade.subjectShort))
                //setContentText(context.getFormattedText(R.string.notify_grade_new_text, grade.teacher))
                //setSubText()
                //setTicker()
                //setUsesChronometer()
                //setNumber()
                //setShowWhen(true)
                //setStyle()

                setSmallIcon(R.drawable.ic_notify_grades)
                //setLargeIcon()
                color = notifyColor
                //setColorized(false)


                setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                setDefaults(NotificationCompat.DEFAULT_ALL)
                priority = NotificationCompat.PRIORITY_HIGH

                setAutoCancel(false) // will be canceled by GradesFragment
                setCategory(NotificationCompat.CATEGORY_EVENT)

                setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                //setTimeoutAfter()
                //setOngoing()
                //setPublicVersion()

                //addAction()
            }

    override fun createNotification(context: Context, group: NotifyGroup, notifyId: NotifyId,
                                    data: Bundle): NotificationCompat.Builder {
        val grade = readDataGrade(data)
                ?: throw IllegalArgumentException("Data doesn't contains grade")
        val color = grade.valueColor

        return buildNotificationBase(context, group, color).apply {
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
                    /*val changes = data.getString(PARAM_CHANGES_LIST)
                            ?.let { JSON.parse(StringSerializer.list, it) }
                            ?: throw IllegalArgumentException(
                                    "Data doesn't contains grade's changes list")*/

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
    }

    override fun createSummaryNotification(context: Context, group: NotifyGroup, notifyId: NotifyId,
                                           data: Map<out NotifyId, Bundle>): NotificationCompat.Builder {
        val account = (group as? AccountNotifyGroup)?.account.alsoIfNull {
            Log.e(LOG_TAG, "createSummaryNotification(id=$notifyId, group=$group," +
                    " notifyId=$notifyId, data=$data)",
                    IllegalArgumentException("Group is not AccountNotifyGroup, " +
                            "can't obtain account."))
        }

        val allGrades = data.values.mapNotNull {
            readDataGrade(it).alsoIfNull {
                Log.e(LOG_TAG, "Data doesn't contains grade")
            }
        }
        val subjects = allGrades.map { it.subjectShort }.distinct().joinToString(", ")
        val color = allGrades.averageColor


        val title = context.getText(R.string.notify_grade_summary_title)
        val text = context.getFormattedText(R.string.notify_grade_summary_text, subjects)

        return buildNotificationBase(context, group, color).apply {
            setContentTitle(title)
            setContentText(text)
            setStyle(
                    NotificationCompat.InboxStyle()
                            .setSummaryText(account?.name ?: title)
                            .setBigContentTitle(title)
                            .also { n ->
                                allGrades.forEach {
                                    n.addLine(
                                            context.getFormattedText(
                                                    R.string.notify_grade_summary_line,
                                                    it.valueToShow, it.subjectShort)
                                    )
                                }
                            }
            )
        }
    }
}
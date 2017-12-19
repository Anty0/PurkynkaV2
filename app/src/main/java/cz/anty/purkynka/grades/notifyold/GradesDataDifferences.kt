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

package cz.anty.purkynka.grades.notifyold

import android.accounts.AccountManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.MainActivity
import cz.anty.purkynka.PrefNames.*
import cz.anty.purkynka.R
import cz.anty.purkynka.accounts.AccountsHelper
import cz.anty.purkynka.grades.GradesFragment
import cz.anty.purkynka.grades.data.Grade
import eu.codetopic.java.utils.JavaExtensions.kSerializer
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.data.preferences.VersionedPreferencesData
import eu.codetopic.utils.data.preferences.preference.BasePreference
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs
import eu.codetopic.utils.AndroidExtensions.getFormattedText
import eu.codetopic.utils.data.preferences.preference.KotlinSerializedPreference
import eu.codetopic.utils.ids.Identifiers
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.PairSerializer
import kotlinx.serialization.map

/**
 * @author anty
 */
@Deprecated("Use NotificationsManager instead")
class GradesDataDifferences private constructor(context: Context) :
        VersionedPreferencesData<SharedPreferences>(context,
                BasicSharedPreferencesProvider(context, FILE_NAME_GRADES_DATA_DIFFERENCES),
                SAVE_VERSION) { // TODO: create NotificationsManager and use it to replace this class

    companion object : PreferencesCompanionObject<GradesDataDifferences>(GradesDataDifferences.LOG_TAG,
            ::GradesDataDifferences, GradesDataDifferences::Getter) {

        private const val LOG_TAG = "GradesDataDifferences"
        private const val SAVE_VERSION = 0

        private const val NOTIFY_BASE_STR_ID = "cz.anty.purkynka.grades.save.$LOG_TAG.notification"
        private const val NOTIFY_ID_SUMMARY = 1
        private val NOTIFY_TYPE_GRADES_DIFF = Identifiers.Type("$LOG_TAG.NOTIFICATION_ID", min = 2)

        fun requestClear(context: Context, accountId: String) {
            context.sendBroadcast(Intent(context, GradesClearDifferencesReceiver::class.java)
                    .putExtra(GradesClearDifferencesReceiver.EXTRA_ACCOUNT_ID, accountId))
        }
    }

    override fun onCreate() {
        super.onCreate()
        val accountManager = AccountManager.get(context)
        AccountsHelper.getAllAccounts(accountManager).forEach {
            updateDiffNotifications(AccountsHelper.getAccountId(accountManager, it))
        }
    }

    override fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
        when (from) {
            -1 -> {
                // First start, nothing to do
            }
        } // No more versions yet
    }

    private fun formatNotifyChannelId(accountId: String) = "$NOTIFY_BASE_STR_ID.channelId(ID={$accountId})"

    private fun formatNotifyTag(accountId: String) = "$NOTIFY_BASE_STR_ID.tag(ID={$accountId})"

    private fun formatNotifyGroup(accountId: String) = "$NOTIFY_BASE_STR_ID.group(ID={$accountId})"

    private val addedPreference = KotlinSerializedPreference<Map<Int, Grade>>(
            GRADES_ADDED,
            (IntSerializer to kSerializer<Grade>()).map,
            accessProvider,
            ::mutableMapOf
    )

    private val modifiedPreference = KotlinSerializedPreference<Map<Int, Pair<Grade, Grade>>>(
            GRADES_MODIFIED,
            (IntSerializer to PairSerializer<Grade, Grade>(kSerializer(), kSerializer())).map,
            accessProvider,
            ::mutableMapOf
    )

    private fun <T> BasePreference<Map<Int, T>, *>.addWithIds(accountId: String, toAdd: List<T>) {
        getValue(this@GradesDataDifferences, accountId).toMutableMap()
                .apply {
                    putAll(toAdd.map { Identifiers.next(NOTIFY_TYPE_GRADES_DIFF) to it })
                }
                .let { setValue(this@GradesDataDifferences, accountId, it) }
    }

    private fun <T> BasePreference<Map<Int, T>, *>.cancelAll(accountId: String) {
        val tag = formatNotifyTag(accountId)
        val notifyManager = NotificationManagerCompat.from(context)

        getValue(this@GradesDataDifferences, accountId).toMutableMap()
                .apply {
                    forEach { notifyManager.cancel(tag, it.key) }
                    clear()
                }
                .let { setValue(this@GradesDataDifferences, accountId, it) }
    }

    private fun <T> BasePreference<Map<Int, T>, *>.cancel(accountId: String, notificationId: Int): T? {
        val tag = formatNotifyTag(accountId)

        return getValue(this@GradesDataDifferences, accountId).toMutableMap().let {
            it.remove(notificationId)?.apply {
                setValue(this@GradesDataDifferences, accountId, it)
                NotificationManagerCompat.from(context).cancel(tag, notificationId)
            }
        }
    }

    internal fun notifyLaunchReceived(accountId: String, notificationId: Int) {
        if (notificationId == NOTIFY_ID_SUMMARY) {
            MainActivity.start(context, GradesFragment::class.java)
            return
        }

        val grade = addedPreference.getValue(this, accountId).let {
            it[notificationId] ?:
                    modifiedPreference.getValue(this, accountId).let {
                        it[notificationId]?.second
                    }
        }

        if (grade == null) {
            Log.w(LOG_TAG, "notifyLaunchReceived(accountId=$accountId, notificationId=$notificationId) -> " +
                    "received request to launch unknown notification.")
            return
        }

        // TODO: show grade activity rather then all grades fragment
        MainActivity.start(context, GradesFragment::class.java)
    }

    internal fun notifyDeleteReceived(accountId: String, notificationId: Int) {
        if (notificationId == NOTIFY_ID_SUMMARY) {
            // Summary notification removed. Nothing to do...
            // (Other notifications will be removed by system and this method will be called for all of them automatically.)
            return
        }


        val grade = addedPreference.cancel(accountId, notificationId) ?:
                modifiedPreference.cancel(accountId, notificationId)?.second

        if (grade == null) {
            Log.w(LOG_TAG, "notifyDeleteReceived(accountId=$accountId, notificationId=$notificationId) -> " +
                    "received request to remove unknown notification.")
            return
        }

        updateDiffNotifications(accountId)
    }

    fun addNewDiffs(accountId: String, added: List<Grade>, modified: List<Pair<Grade, Grade>>) {
        Log.d(LOG_TAG, "addNewDiffs(accountId=$accountId, added=$added, modified=$modified)")

        addedPreference.addWithIds(accountId, added)
        modifiedPreference.addWithIds(accountId, modified)

        updateDiffNotifications(accountId)
    }

    fun clearDiffs(accountId: String) {
        Log.d(LOG_TAG, "clearDiffs(accountId=$accountId)")

        addedPreference.cancelAll(accountId)
        modifiedPreference.cancelAll(accountId)

        updateDiffNotifications(accountId)
    }

    private fun prepareChannel(accountId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channelId = formatNotifyChannelId(accountId)

        val notifyManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifyManager.createNotificationChannel(
                NotificationChannel(
                        channelId,
                        context.getFormattedText(R.string.notify_channel_grades_add, accountId),
                        NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableLights(true)
                    enableVibration(true)
                    setBypassDnd(false)
                    setShowBadge(true)
                    this.lightColor = ContextCompat.getColor(context, R.color.colorPrimaryGrades)
                }
        )
    }

    private fun buildNotificationBase(accountId: String, notificationId: Int, channelId: String, group: String): NotificationCompat.Builder =
            NotificationCompat.Builder(context, channelId).apply {
                setSmallIcon(R.mipmap.ic_launcher_grades)
                /*setContentTitle(context.getFormattedText(R.string.notify_grade_new_title,
                            grade.valueToShow, grade.subjectShort))
                  setContentText(context.getFormattedText(R.string.notify_grade_new_text, grade.teacher))*/
                //setSubText()
                //setTicker()
                //setUsesChronometer()
                //setNumber()
                /*setWhen(grade.date.time)*/
                //setShowWhen(true)
                //setStyle()

                setContentIntent(PendingIntent.getBroadcast(context, notificationId,
                        Intent(context, GradeNotificationLaunchReceiver::class.java)
                                .putExtra(GradeNotificationLaunchReceiver.EXTRA_ACCOUNT_ID, accountId)
                                .putExtra(GradeNotificationLaunchReceiver.EXTRA_NOTIFICATION_ID, notificationId),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                setDeleteIntent(PendingIntent.getBroadcast(context, notificationId,
                        Intent(context, GradeNotificationDeleteReceiver::class.java)
                                .putExtra(GradeNotificationDeleteReceiver.EXTRA_ACCOUNT_ID, accountId)
                                .putExtra(GradeNotificationDeleteReceiver.EXTRA_NOTIFICATION_ID, notificationId),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                setAutoCancel(true)

                setGroup(group)
                setGroupSummary(false)
                setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)

                setCategory(NotificationCompat.CATEGORY_EVENT)

                //setColorized(false)
                color = ContextCompat.getColor(context, R.color.colorPrimaryGrades)
                //setLargeIcon()

                setOnlyAlertOnce(true)
                setDefaults(NotificationCompat.DEFAULT_ALL)
                //setLights()
                //setVibrate()

                setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                //setTimeoutAfter()
                //setOngoing()
                //setPublicVersion()

                //addAction()
                extend(NotificationCompat.WearableExtender()
                        .setHintContentIntentLaunchesActivity(true))

                //setContentInfo()
                //setBadgeIconType()
                //setContent()
                //setCustomBigContentView()
                //setCustomContentView()
                //setCustomHeadsUpContentView()
                //setExtras()
                //setFullScreenIntent()
                //setLocalOnly()
                //setPriority()
                //setProgress()
                //setRemoteInputHistory()
                //setShortcutId()
                //setSortKey()
                //setSound(null)
                //addExtras()
                //addPerson()
            }

    private fun updateDiffNotifications(accountId: String) {
        prepareChannel(accountId)

        val channelId = formatNotifyChannelId(accountId)
        val tag = formatNotifyTag(accountId)
        val group = formatNotifyGroup(accountId)

        val notifyManager = NotificationManagerCompat.from(context)

        val addedGrades = addedPreference.getValue(this, accountId)
        val modifiedGrades = modifiedPreference.getValue(this, accountId)

        addedGrades.forEach {
            val notifyId = it.key
            val grade = it.value

            val notification = buildNotificationBase(accountId, notifyId, channelId, group)
                    .setContentTitle(context.getFormattedText(R.string.notify_grade_new_title,
                            grade.valueToShow, grade.subjectShort))
                    .setContentText(context.getFormattedText(
                            if (grade.note.isEmpty()) R.string.notify_grade_new_text
                            else R.string.notify_grade_new_text_note,
                            grade.note, grade.teacher
                    ))
                    //.setWhen(grade.date.time)
                    .build()

            notifyManager.notify(tag, notifyId, notification)
        }

        modifiedGrades.forEach {
            val notifyId = it.key
            val oldGrade = it.value.first
            val newGrade = it.value.second

            // TODO: show notification based on which differences are between old and new grade

            val notification = buildNotificationBase(accountId, notifyId, channelId, group)
                    .setContentTitle(context.getFormattedText(R.string.notify_grade_modified_title,
                            newGrade.valueToShow, newGrade.subjectShort))
                    .setContentText(context.getFormattedText(
                            if (newGrade.note.isEmpty()) R.string.notify_grade_modified_text
                            else R.string.notify_grade_modified_text_note,
                            newGrade.note, newGrade.teacher
                    ))
                    .build()

            notifyManager.notify(tag, notifyId, notification)
        }

        val allGrades = addedGrades.values + modifiedGrades.values.map { it.second }
        val subjects = allGrades.map { it.subjectShort }.distinct().joinToString(", ")

        if (allGrades.isEmpty()) {
            notifyManager.cancel(tag, NOTIFY_ID_SUMMARY)
        } else {
            val notification = buildNotificationBase(accountId, NOTIFY_ID_SUMMARY, channelId, group)
                    .setContentTitle(context.getText(R.string.notify_grade_summary_title))
                    .setContentText(context.getFormattedText(R.string.notify_grade_summary_text, subjects))
                    .setStyle(
                            NotificationCompat.InboxStyle()
                                    .setBigContentTitle(context.getText(R.string.notify_grade_summary_title))
                                    .setSummaryText(context.getFormattedText(R.string.notify_grade_summary_text, subjects))
                                    .also { n ->
                                        allGrades.forEach {
                                            n.addLine(context.getFormattedText(R.string.notify_grade_summary_line,
                                                    it.valueToShow, it.subjectShort))
                                        }
                                    }
                    )
                    .setGroupSummary(true)
                    .build()
            notifyManager.notify(tag, NOTIFY_ID_SUMMARY, notification)
        }
    }

    private class Getter : PreferencesGetterAbs<GradesDataDifferences>() {

        override fun get() = instance

        override val dataClass: Class<out GradesDataDifferences>
            get() = GradesDataDifferences::class.java
    }
}
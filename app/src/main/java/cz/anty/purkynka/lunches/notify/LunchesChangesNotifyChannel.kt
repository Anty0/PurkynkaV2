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

package cz.anty.purkynka.lunches.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.MainActivity
import cz.anty.purkynka.R
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.account.save.ActiveAccount
import cz.anty.purkynka.lunches.LunchesOrderFragment
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import cz.anty.purkynka.lunches.data.LunchOptionsGroup.Companion.dateStrShort
import cz.anty.purkynka.lunches.ui.LunchOptionsGroupActivity
import eu.codetopic.java.utils.alsoIfNull
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.getFormattedQuantityText
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.ids.Identifiers
import eu.codetopic.utils.ids.Identifiers.Companion.nextId
import eu.codetopic.utils.notifications.manager.data.NotifyId
import eu.codetopic.utils.notifications.manager.util.NotifyGroup
import eu.codetopic.utils.notifications.manager.util.SummarizedNotifyChannel
import kotlinx.serialization.json.JSON

/**
 * @author anty
 */
class LunchesChangesNotifyChannel : SummarizedNotifyChannel(ID, checkForIdOverrides = true) {

    companion object {

        private const val LOG_TAG = "LunchesChangesNotifyChannel"
        const val ID = "LUNCHES_CHANGES"

        private val idType = Identifiers.Type(ID)

        private const val PARAM_LUNCH_OPTIONS_GROUP = "LUNCH_OPTIONS_GROUP"

        fun dataFor(lunchOptionsGroup: LunchOptionsGroup): Bundle = Bundle().apply {
            putString(PARAM_LUNCH_OPTIONS_GROUP, JSON.stringify(lunchOptionsGroup))
        }

        fun readData(data: Bundle): LunchOptionsGroup? =
                data.getString(PARAM_LUNCH_OPTIONS_GROUP)?.let { JSON.parse(it) }
    }

    override fun nextId(context: Context, group: NotifyGroup,
                        data: Bundle): Int = idType.nextId()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel(context: Context, combinedId: String): NotificationChannel =
            NotificationChannel(
                    combinedId,
                    context.getText(R.string.notify_channel_name_lunches_changes),
                    NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(true)
                enableVibration(true)
                setBypassDnd(false)
                setShowBadge(true)
                lightColor = ContextCompat.getColor(context, R.color.colorPrimaryLunches)
            }

    override fun handleContentIntent(context: Context, group: NotifyGroup,
                                     notifyId: NotifyId, data: Bundle) {
        val (account, accountId) = (group as? AccountNotifyGroup)
                ?.let { it.account to it.accountId }
                .alsoIfNull {
                    Log.e(LOG_TAG, "handleContentIntent(id=$notifyId, group=$group," +
                            " notifyId=$notifyId, data=$data)",
                            IllegalArgumentException("Group is not AccountNotifyGroup, " +
                                    "can't change ActiveAccount to correct account."))
                }
                ?: null to null

        val lunchGroup = readData(data).alsoIfNull {
            Log.e(LOG_TAG, "handleContentIntent(id=$notifyId, group=$group," +
                    " notifyId=$notifyId, data=$data)",
                    IllegalArgumentException("Data doesn't contains lunchOptionsGroup"))
        }

        account?.let { ActiveAccount.set(it) }

        if (accountId != null && lunchGroup != null) {
            context.startActivities(arrayOf(
                    MainActivity.getStartIntent(context, LunchesOrderFragment::class.java)
                            .addFlags(FLAG_ACTIVITY_NEW_TASK),
                    LunchOptionsGroupActivity.getStartIntent(context, accountId, lunchGroup)
            ))
        } else {
            MainActivity.start(context, LunchesOrderFragment::class.java)
        }
    }

    override fun handleSummaryContentIntent(context: Context, group: NotifyGroup,
                                            notifyId: NotifyId, data: Map<out NotifyId, Bundle>) {
        val account = (group as? AccountNotifyGroup)?.account.alsoIfNull {
            Log.e(LOG_TAG, "handleSummaryContentIntent(id=$notifyId, group=$group," +
                    " notifyId=$notifyId, data=$data)",
                    IllegalArgumentException("Group is not AccountNotifyGroup, " +
                            "can't change ActiveAccount to correct account."))
        }

        account?.let { ActiveAccount.set(it) }

        MainActivity.start(context, LunchesOrderFragment::class.java)
    }

    private fun buildNotificationBase(context: Context, group: NotifyGroup): NotificationCompat.Builder =
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

                setSmallIcon(R.drawable.ic_notify_lunches_order)
                //setLargeIcon()
                color = ContextCompat.getColor(context, R.color.colorPrimaryLunches)
                //setColorized(false)


                setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                setDefaults(NotificationCompat.DEFAULT_ALL)
                priority = NotificationCompat.PRIORITY_DEFAULT

                setAutoCancel(false) // will be canceled by LunchesOrderFragment
                setCategory(NotificationCompat.CATEGORY_EVENT)

                setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                //setTimeoutAfter()
                //setOngoing()
                //setPublicVersion()

                //addAction()
            }

    override fun createNotification(context: Context, group: NotifyGroup, notifyId: NotifyId,
                                    data: Bundle): NotificationCompat.Builder {
        val lunchGroup = readData(data)
                ?: throw IllegalArgumentException("Data doesn't contains lunchOptionsGroup")

        return buildNotificationBase(context, group).apply {
            setContentTitle(context.getFormattedText(
                    R.string.notify_lunch_new_title,
                    lunchGroup.dateStrShort
            ))
            val optionsCount = lunchGroup.options?.count() ?: 0
            setContentText(context.getFormattedQuantityText(
                    R.plurals.notify_lunch_new_text,
                    optionsCount, optionsCount
            ))
            setStyle(
                    NotificationCompat.BigTextStyle()
                            .bigText(
                                    lunchGroup.options
                                            ?.takeIf { it.isNotEmpty() }
                                            ?.joinToString(
                                                    separator = "\n - ",
                                                    prefix = " - "
                                            ) { it.name }
                                            ?: context.getText(
                                                    R.string.notify_lunch_new_big_text_no_options
                                            )
                    )
            )
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

        val allLunchesGroups = data.values.mapNotNull {
            readData(it).alsoIfNull {
                Log.e(LOG_TAG, "Data doesn't contains lunchOptionsGroup")
            }
        }

        val minDate = allLunchesGroups.minBy { it.date }?.dateStrShort
        val maxDate = allLunchesGroups.maxBy { it.date }?.dateStrShort

        val title = context.getText(R.string.notify_lunches_new_summary_title)
        val text = context.getFormattedText(
                if (minDate != null && maxDate != null) {
                    if (minDate == maxDate) {
                        R.string.notify_lunches_new_summary_text_single_date
                    } else R.string.notify_lunches_new_summary_text
                } else R.string.notify_lunches_new_summary_text_no_date,
                minDate ?: "", maxDate ?: ""
        )

        val lines = allLunchesGroups.map {
            val optionsCount = it.options?.count() ?: 0
            return@map context.getFormattedQuantityText(
                    R.plurals.notify_lunches_new_summary_line,
                    optionsCount, optionsCount, it.dateStrShort
            )
        }

        return buildNotificationBase(context, group).apply {

            setContentTitle(title)
            setContentText(text)
            setStyle(
                    NotificationCompat.InboxStyle()
                            .setSummaryText(account?.name ?: title)
                            .setBigContentTitle(title)
                            .also { n -> lines.forEach { n.addLine(it) } }
            )
        }
    }
}
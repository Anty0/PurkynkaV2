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
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.MainActivity
import cz.anty.purkynka.R
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.account.save.ActiveAccount
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel
import cz.anty.purkynka.lunches.LunchesOrderFragment
import eu.codetopic.java.utils.alsoIfNull
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.ids.Identifiers
import eu.codetopic.utils.ids.Identifiers.Companion.nextId
import eu.codetopic.utils.notifications.manager.combinedIdFor
import eu.codetopic.utils.notifications.manager.data.NotifyId
import eu.codetopic.utils.notifications.manager.util.NotifyChannel
import eu.codetopic.utils.notifications.manager.util.NotifyGroup
import org.jetbrains.anko.bundleOf

/**
 * @author anty
 */
class LunchesBurzaWatcherResultChannel : NotifyChannel(ID, true) {

    companion object {

        private const val LOG_TAG = "LunchesBurzaWatcherResultChannel"
        const val ID = "${BuildConfig.APPLICATION_ID}.lunches.notify.burza_watcher_result"

        private val idType = Identifiers.Type(GradesChangesNotifyChannel.ID)

        private const val PARAM_SUCCESS = "SUCCESS"

        fun dataFor(success: Boolean): Bundle = bundleOf(
                PARAM_SUCCESS to success
        )

        fun readDataSuccess(data: Bundle): Boolean? = data
                .takeIf { it.containsKey(PARAM_SUCCESS) }
                ?.getBoolean(PARAM_SUCCESS)
    }

    override fun nextId(context: Context, group: NotifyGroup,
                        data: Bundle): Int = idType.nextId()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel(context: Context, combinedId: String): NotificationChannel =
            NotificationChannel(
                    combinedId,
                    context.getText(R.string.notify_channel_name_lunches_burza_watcher_result),
                    NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                setBypassDnd(false)
                setShowBadge(true)
                this.lightColor = ContextCompat.getColor(context, R.color.colorPrimaryLunches)
            }

    override fun handleContentIntent(context: Context, group: NotifyGroup,
                                     notifyId: NotifyId, data: Bundle) {
        val accountId = (group as? AccountNotifyGroup)?.accountId.alsoIfNull {
            Log.e(LOG_TAG, "handleContentIntent(id=$notifyId, group=$group," +
                    " notifyId=$notifyId, data=$data)",
                    IllegalArgumentException("Group is not AccountNotifyGroup, " +
                            "can't change ActiveAccount to correct account."))
        }

        accountId?.let { ActiveAccount.set(it) }

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

                setSmallIcon(R.drawable.ic_notify_lunches_burza_watcher)
                //setLargeIcon()
                color = ContextCompat.getColor(context, R.color.colorPrimaryLunches)
                //setColorized(true)

                setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
                setDefaults(NotificationCompat.DEFAULT_ALL)
                priority = NotificationCompat.PRIORITY_HIGH

                setAutoCancel(false) // will be canceled automatically
                setCategory(NotificationCompat.CATEGORY_EVENT)

                setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                //setTimeoutAfter()
                //setOngoing(true)
                //setPublicVersion()

                //addAction()
            }

    override fun createNotification(context: Context, group: NotifyGroup, notifyId: NotifyId,
                                    data: Bundle): NotificationCompat.Builder {
        return buildNotificationBase(context, group).apply {
            val success = readDataSuccess(data)
                    ?: throw IllegalArgumentException("Data doesn't contains success")
            val account = (group as? AccountNotifyGroup)?.account.alsoIfNull {
                Log.e(LOG_TAG, "createNotification(id=$notifyId, group=$group," +
                        " notifyId=$notifyId, data=$data)",
                        IllegalArgumentException("Group is not AccountNotifyGroup, " +
                                "can't show account name in notification."))
            }

            val isMultiAccount = Accounts.getAll(context).size > 1

            setContentTitle(context.getText(
                    if (success) R.string.notify_lunches_watcher_result_title_success
                    else R.string.notify_lunches_watcher_result_title_fail
            ))
            setContentText(
                    if (isMultiAccount && account != null)
                        context.getFormattedText(
                                R.string.notify_lunches_watcher_result_text_multi_user,
                                account.name
                        )
                    else context.getText(R.string.notify_lunches_watcher_result_text)
            )
        }
    }
}
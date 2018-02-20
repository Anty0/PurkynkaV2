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
import cz.anty.purkynka.MainActivity
import cz.anty.purkynka.R
import cz.anty.purkynka.lunches.LunchesBurzaWatcherFragment
import eu.codetopic.utils.notifications.manager.data.NotifyId
import eu.codetopic.utils.notifications.manager.util.NotifyChannel
import eu.codetopic.utils.notifications.manager.util.NotifyGroup

/**
 * @author anty
 */
class LunchesBurzaWatcherStatusChannel : NotifyChannel(ID, false) {

    companion object {

        private const val LOG_TAG = "LunchesBurzaWatcherStatusChannel"
        const val ID = "LUNCHES_BURZA_WATCHER_STATUS"

        const val NOTIFY_ID = 73528
    }

    override fun nextId(context: Context, group: NotifyGroup, data: Bundle): Int = NOTIFY_ID

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel(context: Context, combinedId: String): NotificationChannel =
            NotificationChannel(
                    combinedId,
                    context.getText(R.string.notify_channel_name_lunches_burza_watcher_status),
                    NotificationManager.IMPORTANCE_LOW
            ).apply {
                enableLights(false)
                enableVibration(false)
                setBypassDnd(false)
                setShowBadge(false)
                this.lightColor = ContextCompat.getColor(context, R.color.colorPrimaryLunches)
            }

    override fun handleContentIntent(context: Context, group: NotifyGroup,
                                     notifyId: NotifyId, data: Bundle) {
        MainActivity.start(context, LunchesBurzaWatcherFragment::class.java)
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
                color = ContextCompat.getColor(context, R.color.colorPrimaryDarkLunches)
                setColorized(true)

                setProgress(0, 0, true)

                setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
                //setDefaults(NotificationCompat.DEFAULT_ALL)
                priority = NotificationCompat.PRIORITY_LOW

                setAutoCancel(false) // will be canceled automatically
                setCategory(NotificationCompat.CATEGORY_STATUS)

                setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                //setTimeoutAfter()
                setOngoing(true)
                //setPublicVersion()

                //addAction()
            }

    override fun createNotification(context: Context, group: NotifyGroup, notifyId: NotifyId,
                                    data: Bundle): NotificationCompat.Builder {
        return buildNotificationBase(context, group).apply {
            setContentTitle(context.getText(R.string.notify_lunches_watcher_status_title))
            setContentText(context.getText(R.string.notify_lunches_watcher_status_text))
        }
    }
}
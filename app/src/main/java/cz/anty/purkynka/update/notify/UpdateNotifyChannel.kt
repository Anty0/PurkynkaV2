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

package cz.anty.purkynka.update.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.Constants.ICON_UPDATE
import cz.anty.purkynka.R
import eu.codetopic.java.utils.JavaExtensions.letIf
import eu.codetopic.utils.AndroidExtensions.getIconics
import eu.codetopic.utils.AndroidExtensions.getFormattedText
import eu.codetopic.utils.notifications.manager.data.NotifyId
import eu.codetopic.utils.notifications.manager.util.NotifyChannel
import eu.codetopic.utils.notifications.manager.util.NotifyGroup

/**
 * @author anty
 */
class UpdateNotifyChannel : NotifyChannel(ID, false) {

    companion object {

        private const val LOG_TAG = "UpdateNotifyChannel"
        const val ID = "cz.anty.purkynka.update.notify.$LOG_TAG"

        private const val PARAM_VERSION_CODE = "VERSION_CODE"
        private const val PARAM_VERSION_NAME = "VERSION_NAME"

        fun dataForVersion(code: Int, name: String): Bundle = Bundle().apply {
            putInt(PARAM_VERSION_CODE, code)
            putString(PARAM_VERSION_NAME, name)
        }

        fun readDataVersionCode(data: Bundle): Int? =
                data.getInt(PARAM_VERSION_CODE, -1)
                        .letIf({ it == -1 }) { null }

        fun readDataVersionName(data: Bundle): String? =
                data.getString(PARAM_VERSION_NAME)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel(context: Context, combinedId: String): NotificationChannel =
            android.app.NotificationChannel(
                    combinedId,
                    context.getText(R.string.notify_channel_update_available),
                    NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                setBypassDnd(false)
                setShowBadge(true)
                this.lightColor = ContextCompat.getColor(context, R.color.colorPrimary)
            }

    override fun nextId(context: Context, group: NotifyGroup,
                        data: Bundle): Int = 0 // = Replace existing notification

    override fun handleContentIntent(context: Context, group: NotifyGroup,
                                     notifyId: NotifyId, data: Bundle) {
        TODO("implement")
    }

    private fun buildNotificationBase(context: Context,
                                      group: NotifyGroup): NotificationCompat.Builder =
            NotificationCompat.Builder(context, combinedIdFor(group)).apply {
                //setContentTitle()
                //setContentText()
                //setSubText()
                //setTicker()
                //setUsesChronometer()
                //setNumber()
                //setShowWhen(true)
                //setStyle()

                setSmallIcon(R.drawable.ic_notify_update)
                /*setLargeIcon(
                        context.getIconics(ICON_UPDATE)
                                .sizeDp(24)
                                .colorRes(R.color.colorPrimary)
                                .toBitmap()
                )*/
                color = ContextCompat.getColor(context, R.color.colorPrimary)
                setColorized(true)

                setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
                setDefaults(NotificationCompat.DEFAULT_ALL)
                priority = NotificationCompat.PRIORITY_HIGH

                setAutoCancel(true)
                setCategory(NotificationCompat.CATEGORY_EVENT)

                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //setTimeoutAfter()
                //setOngoing()
                //setPublicVersion()

                //addAction()
            }

    override fun createNotification(context: Context, group: NotifyGroup, notifyId: NotifyId,
                                    data: Bundle): NotificationCompat.Builder =
            buildNotificationBase(context, group).apply {
                val versionName = readDataVersionName(data)
                        ?: context.getString(R.string.notify_update_available_version_unknown)

                setContentTitle(context.getText(R.string.notify_update_available_title))
                setContentText(context.getFormattedText(
                        R.string.notify_update_available_text,
                        versionName
                ))
            }
}
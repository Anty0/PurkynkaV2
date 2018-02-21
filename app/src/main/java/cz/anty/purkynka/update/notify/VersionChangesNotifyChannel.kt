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
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.MainActivity
import cz.anty.purkynka.R
import cz.anty.purkynka.update.CHANGELOG_MAP
import cz.anty.purkynka.update.getChangesText
import cz.anty.purkynka.update.ui.ChangelogActivity
import cz.anty.purkynka.update.ui.VersionChangesActivity
import eu.codetopic.java.utils.alsoIfNull
import eu.codetopic.java.utils.letIf
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.ids.Identifiers
import eu.codetopic.utils.ids.Identifiers.Companion.nextId
import eu.codetopic.utils.notifications.manager.data.NotifyId
import eu.codetopic.utils.notifications.manager.util.NotifyGroup
import eu.codetopic.utils.notifications.manager.util.SummarizedNotifyChannel

/**
 * @author anty
 */
class VersionChangesNotifyChannel : SummarizedNotifyChannel(ID, checkForIdOverrides = false) {

    companion object {

        private const val LOG_TAG = "VersionChangesNotifyChannel"
        const val ID = "cz.anty.purkynka.update.notify.$LOG_TAG"

        private val idType = Identifiers.Type(ID)

        private const val PARAM_VERSION_CODE = "VERSION_CODE"

        fun dataFor(versionCode: Int): Bundle = Bundle().apply {
            putInt(PARAM_VERSION_CODE, versionCode)
        }

        fun readDataVersionCode(data: Bundle): Int? =
                data.getInt(PARAM_VERSION_CODE, -1)
                        .letIf({ it == -1 }) { null }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel(context: Context, combinedId: String): NotificationChannel =
            android.app.NotificationChannel(
                    combinedId,
                    context.getText(R.string.notify_channel_changelog),
                    NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(true)
                enableVibration(false)
                setBypassDnd(false)
                setShowBadge(true)
                this.lightColor = ContextCompat.getColor(context, R.color.colorPrimary)
            }

    override fun nextId(context: Context, group: NotifyGroup,
                        data: Bundle): Int = idType.nextId()

    override fun handleContentIntent(context: Context, group: NotifyGroup,
                                     notifyId: NotifyId, data: Bundle) {
        val versionCode = readDataVersionCode(data).alsoIfNull {
            Log.e(LOG_TAG, "handleContentIntent(id=$notifyId, group=$group," +
                    " notifyId=$notifyId, data=$data)",
                    IllegalArgumentException("Data doesn't contain versionCode"))
        }

        if (versionCode != null) {
            context.startActivities(arrayOf(
                    MainActivity.getStartIntent(context)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    VersionChangesActivity.getIntent(context, versionCode)
            ))
        } else {
            context.startActivities(arrayOf(
                    MainActivity.getStartIntent(context)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    ChangelogActivity.getIntent(context)
            ))
        }
    }

    override fun handleSummaryContentIntent(context: Context, group: NotifyGroup,
                                            notifyId: NotifyId, data: Map<out NotifyId, Bundle>) {
        context.startActivities(arrayOf(
                MainActivity.getStartIntent(context)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ChangelogActivity.getIntent(context)
        ))
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

                setSmallIcon(R.drawable.ic_notify_version_changes)
                /*setLargeIcon(
                        context.getIconics(ICON_UPDATE)
                                .sizeDp(24)
                                .colorRes(R.color.colorPrimary)
                                .toBitmap()
                )*/
                color = ContextCompat.getColor(context, R.color.colorPrimary)
                setColorized(true)

                setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                setDefaults(NotificationCompat.DEFAULT_LIGHTS)
                priority = NotificationCompat.PRIORITY_DEFAULT

                setAutoCancel(false) // Will be canceled by VersionChangesActivity
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
                val versionCode = readDataVersionCode(data)
                        ?: throw IllegalArgumentException("Data doesn't contain versionCode")
                val versionInfo = CHANGELOG_MAP[versionCode]
                        ?: throw RuntimeException("VersionInfo not found for versionCode '$versionCode'")

                setContentTitle(context.getText(R.string.notify_version_changes_title))
                setContentText(context.getFormattedText(
                        R.string.notify_version_changes_text,
                        versionInfo.name
                ))
                setStyle(
                        NotificationCompat.BigTextStyle()
                                .setBigContentTitle(context.getFormattedText(
                                        R.string.notify_version_changes_big_title,
                                        versionInfo.name
                                ))
                                .bigText(versionInfo.getChangesText(context))
                )
            }

    override fun createSummaryNotification(context: Context, group: NotifyGroup, notifyId: NotifyId,
                                           data: Map<out NotifyId, Bundle>): NotificationCompat.Builder {

        val allVersions = data.values.mapNotNull map@ {
            val code = readDataVersionCode(it).alsoIfNull {
                Log.e(LOG_TAG, "Data doesn't contains versionCode")
            } ?: return@map null

            val info = CHANGELOG_MAP[code].alsoIfNull {
                Log.e(LOG_TAG, "VersionInfo not found for versionCode '$code'")
            } ?: return@map null

            return@map code to info
        }

        val versions = allVersions.joinToString(", ") { it.second.name }

        val title = context.getText(R.string.notify_version_changes_summary_title)
        val text = context.getFormattedText(R.string.notify_version_changes_summary_text, versions)

        return buildNotificationBase(context, group).apply {
            setContentTitle(title)
            setContentText(text)
            setStyle(
                    NotificationCompat.InboxStyle()
                            .setSummaryText("")
                            .setBigContentTitle(title)
                            .also { n ->
                                allVersions.forEach {
                                    val (_, versionInfo) = it
                                    n.addLine(
                                            context.getFormattedText(
                                                    R.string.notify_version_changes_summary_line,
                                                    versionInfo.name
                                            )
                                    )
                                }
                            }
            )
        }
    }
}
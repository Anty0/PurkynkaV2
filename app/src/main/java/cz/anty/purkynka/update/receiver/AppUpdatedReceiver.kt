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

package cz.anty.purkynka.update.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.update.CHANGELOG_MAP
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.update.notify.VersionChangesNotifyChannel
import cz.anty.purkynka.update.save.UpdateData
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.notifications.manager.create.MultiNotificationBuilder
import eu.codetopic.utils.notifications.manager.create.NotificationBuilder
import eu.codetopic.utils.notifications.manager.requestShow
import eu.codetopic.utils.notifications.manager.requestShowAll

/**
 * @author anty
 */
class AppUpdatedReceiver : BroadcastReceiver() {

    companion object {

        private const val LOG_TAG = "AppUpdatedReceiver"

        const val ACTION_FAKE_MY_PACKAGE_REPLACED =
                "cz.anty.purkynka.update.FAKE_MY_PACKAGE_REPLACED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        intent.action?.takeIf {
            it == Intent.ACTION_MY_PACKAGE_REPLACED
                    || it == ACTION_FAKE_MY_PACKAGE_REPLACED
        } ?: return

        Log.d(LOG_TAG, "onReceive() -> Received app replaced intent")

        val currentVersion = BuildConfig.VERSION_CODE
        val lastKnownVersion = UpdateData.instance.lastKnownVersion

        if (lastKnownVersion != currentVersion) {
            if (lastKnownVersion != -1) {
                Log.d(LOG_TAG, "onReceive() -> Detected update")

                CHANGELOG_MAP.keys
                        .filter { it in (lastKnownVersion + 1)..currentVersion }
                        .map {
                            VersionChangesNotifyChannel.dataFor(
                                    versionCode = it
                            )
                        }
                        .takeIf { it.isNotEmpty() }
                        ?.also {
                            MultiNotificationBuilder.create(
                                    groupId = UpdateNotifyGroup.ID,
                                    channelId = VersionChangesNotifyChannel.ID
                            ) {
                                persistent = true
                                refreshable = true
                                data = it
                            }.requestShowAll(context)
                        }
            }

            UpdateData.instance.lastKnownVersion = currentVersion
        }
    }
}
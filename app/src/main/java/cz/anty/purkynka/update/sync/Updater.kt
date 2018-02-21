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

package cz.anty.purkynka.update.sync

import android.content.Context
import com.evernote.android.job.Job
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.update.load.UpdateFetcher
import cz.anty.purkynka.update.notify.UpdateNotifyChannel
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.update.save.UpdateData
import eu.codetopic.java.utils.alsoIf
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.UtilsBase
import eu.codetopic.utils.notifications.manager.create.NotificationBuilder
import eu.codetopic.utils.notifications.manager.create.NotificationBuilder.Companion.requestShow

/**
 * @author anty
 */
object Updater {

    private const val LOG_TAG = "Updater"

    fun fetchUpdates(): Job.Result {
        Log.d(LOG_TAG, "fetchUpdates(process=${UtilsBase.Process.name})")

        val code = UpdateFetcher.fetchVersionCode() ?: return Job.Result.FAILURE
        val name = UpdateFetcher.fetchVersionName() ?: return Job.Result.FAILURE

        UpdateData.instance.setLatestVersion(code, name)

        return Job.Result.SUCCESS
    }

    fun fetchUpdatesAndNotify(context: Context): Job.Result =
            fetchUpdates().also checkResult@ {
                when (it) {
                    Job.Result.SUCCESS -> {
                        val currentVersionCode = BuildConfig.VERSION_CODE
                        val latestVersionCode = UpdateData.instance.latestVersionCode
                        if (latestVersionCode == currentVersionCode) return@checkResult

                        Log.b(LOG_TAG, "fetchUpdatesAndNotify() -> Found update" +
                                " -> ($currentVersionCode -> $latestVersionCode)")

                        val latestVersion = UpdateData.instance.latestVersion

                        NotificationBuilder.create(
                                groupId = UpdateNotifyGroup.ID,
                                channelId = UpdateNotifyChannel.ID,
                                init = {
                                    persistent = true
                                    refreshable = true
                                    data = UpdateNotifyChannel.dataForVersion(
                                            code = latestVersion.first,
                                            name = latestVersion.second
                                    )
                                }
                        ).requestShow(context)
                    }
                    else -> {
                        Log.b(LOG_TAG, "fetchUpdatesAndNotify()" +
                                " -> Failed to check for update" +
                                " -> (result=$it)")
                    }
                }
            }
}
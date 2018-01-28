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
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.update.load.UpdateFetcher
import cz.anty.purkynka.update.notify.UpdateNotifyChannel
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.update.save.UpdateData
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.JavaExtensions.alsoIf
import eu.codetopic.utils.notifications.manager2.create.NotificationBuilder
import eu.codetopic.utils.notifications.manager2.create.NotificationBuilder.Companion.requestShow

/**
 * @author anty
 */
class UpdateCheckJob : Job() {

    companion object {

        private const val LOG_TAG = "UpdateCheckJob"

        private const val INTERVAL = 1_000L * 60L * 60L * 6L // 6 hours in milis
        private const val FLEX = 1_000L * 60L * 60L * 1L // 1 hour in milis

        const val TAG = "UPDATE_CHECK"

        fun requestSchedule(context: Context) =
                context.sendBroadcast(UpdateCheckJobScheduleReceiver.getIntent(context))

        fun schedule() {
            Log.d(LOG_TAG, "schedule")

            if (UpdateData.instance.jobScheduleVersion != BuildConfig.VERSION_CODE
                    || JobManager.instance().getAllJobsForTag(TAG).isNotEmpty())
                return

            JobRequest.Builder(TAG)
                    .setPeriodic(INTERVAL, FLEX)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .setUpdateCurrent(true)
                    .build()
                    .schedule()

            UpdateData.instance.jobScheduleVersion = BuildConfig.VERSION_CODE
        }

        fun fetchUpdates(): Result {
            val code = UpdateFetcher.fetchVersionCode() ?: return Result.FAILURE
            val name = UpdateFetcher.fetchVersionName() ?: return Result.FAILURE

            UpdateData.instance.setLatestVersion(code, name)

            return Result.SUCCESS
        }
    }

    override fun onRunJob(params: Params): Result {
        return fetchUpdates().alsoIf({
            it == Result.SUCCESS && UpdateData.instance.latestVersionCode != BuildConfig.VERSION_CODE
        }) {
            Log.w(LOG_TAG, "onRunJob(params=$params) -> Check for update -> Found update ->" +
                    " (${BuildConfig.VERSION_CODE} -> ${UpdateData.instance.latestVersionCode})")

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
    }
}
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

import android.app.Activity
import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.update.load.UpdateFetcher
import cz.anty.purkynka.update.notify.UpdateNotifyChannel
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.update.receiver.UpdateCheckJobScheduleReceiver
import cz.anty.purkynka.update.receiver.UpdateFetchReceiver
import cz.anty.purkynka.update.save.UpdateData
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.JavaExtensions.alsoIf
import eu.codetopic.utils.AndroidExtensions.OrderedBroadcastResult
import eu.codetopic.utils.AndroidExtensions.sendSuspendOrderedBroadcast
import eu.codetopic.utils.UtilsBase
import eu.codetopic.utils.notifications.manager.create.NotificationBuilder
import eu.codetopic.utils.notifications.manager.create.NotificationBuilder.Companion.requestShow

/**
 * @author anty
 */
class UpdateCheckJob : Job() {

    companion object {

        private const val LOG_TAG = "UpdateCheckJob"

        internal const val REQUEST_RESULT_OK = -1
        internal const val REQUEST_RESULT_FAIL = 0
        internal const val REQUEST_RESULT_UNKNOWN = 1

        internal const val REQUEST_EXTRA_RESULT = "EXTRA_RESULT"
        internal const val REQUEST_EXTRA_THROWABLE = "EXTRA_THROWABLE"

        private const val INTERVAL = 1_000L * 60L * 60L * 6L // 6 hours in milis
        private const val FLEX = 1_000L * 60L * 60L * 1L // 1 hour in milis

        const val TAG = "UPDATE_CHECK"

        fun requestSchedule(context: Context) =
                context.sendBroadcast(UpdateCheckJobScheduleReceiver.getIntent(context))

        suspend fun requestSuspendSchedule(context: Context) =
                context.sendSuspendOrderedBroadcast(
                        intent = UpdateCheckJobScheduleReceiver.getIntent(context),
                        initialResult = OrderedBroadcastResult(REQUEST_RESULT_UNKNOWN)
                ).let {
                    when (it.code) {
                        REQUEST_RESULT_OK -> Unit
                        REQUEST_RESULT_FAIL ->
                            throw it.extras?.getSerializable(REQUEST_EXTRA_THROWABLE) as? Throwable
                                    ?: RuntimeException("Unknown fail result received from UpdateCheckJobScheduleReceiver")
                        REQUEST_RESULT_UNKNOWN ->
                            throw RuntimeException("Failed to process broadcast by UpdateCheckJobScheduleReceiver")
                        else -> throw RuntimeException("Unknown resultCode received from UpdateCheckJobScheduleReceiver: ${it.code}")
                    }
                }

        fun schedule() {
            Log.d(LOG_TAG, "schedule")

            if (UpdateData.instance.jobScheduleVersion != BuildConfig.VERSION_CODE
                    || JobManager.instance().getAllJobRequestsForTag(TAG).isNotEmpty())
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
            Log.w(LOG_TAG, "fetchUpdates(process=${UtilsBase.Process.name})")

            val code = UpdateFetcher.fetchVersionCode() ?: return Result.FAILURE
            val name = UpdateFetcher.fetchVersionName() ?: return Result.FAILURE

            UpdateData.instance.setLatestVersion(code, name)

            return Result.SUCCESS
        }

        fun requestFetchUpdates(context: Context) =
                context.sendBroadcast(UpdateFetchReceiver.getIntent(context))

        suspend fun requestSuspendFetchUpdates(context: Context): Result =
                context.sendSuspendOrderedBroadcast(
                        intent = UpdateFetchReceiver.getIntent(context),
                        initialResult = OrderedBroadcastResult(REQUEST_RESULT_UNKNOWN)
                ).let {
                    when (it.code) {
                        REQUEST_RESULT_OK ->
                            it.extras?.getSerializable(REQUEST_EXTRA_RESULT) as? Result
                                    ?: throw RuntimeException("Failed to extract result of UpdateFetchReceiver")
                        REQUEST_RESULT_FAIL ->
                            throw it.extras?.getSerializable(REQUEST_EXTRA_THROWABLE) as? Throwable
                                    ?: RuntimeException("Unknown fail result received from UpdateFetchReceiver")
                        REQUEST_RESULT_UNKNOWN ->
                            throw RuntimeException("Failed to process broadcast by UpdateFetchReceiver")
                        else -> throw RuntimeException("Unknown resultCode received from UpdateFetchReceiver: ${it.code}")
                    }
                }
    }

    override fun onRunJob(params: Params): Result {
        Log.w(LOG_TAG, "onRunJob(params=$params) -> Checking for update")

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
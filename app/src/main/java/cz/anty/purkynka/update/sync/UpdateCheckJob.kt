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
import cz.anty.purkynka.update.receiver.UpdateCheckJobScheduleReceiver
import cz.anty.purkynka.update.save.UpdateData
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.OrderedBroadcastResult
import eu.codetopic.utils.sendSuspendOrderedBroadcast

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
    }

    override fun onRunJob(params: Params): Result {
        Log.w(LOG_TAG, "onRunJob(params=$params) -> Starting UpdateCheckService")

        UpdateCheckService.start(context)

        return Job.Result.SUCCESS
    }
}
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

import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.update.save.UpdateData
import eu.codetopic.java.utils.log.Log

/**
 * @author anty
 */
class UpdateCheckJob : Job() {

    companion object {

        private const val LOG_TAG = "UpdateCheckJob"

        private const val INTERVAL = 1_000L * 60L * 60L * 6L // 6 hours in milis
        private const val FLEX = 1_000L * 60L * 60L * 1L // 1 hour in milis

        const val JOB_TAG = "UPDATE_CHECK"

        fun schedule() {
            Log.d(LOG_TAG, "schedule")

            if (UpdateData.instance.jobScheduleVersion == BuildConfig.VERSION_CODE
                    && JobManager.instance().getAllJobRequestsForTag(JOB_TAG).isNotEmpty())
                return

            try {
                JobRequest.Builder(JOB_TAG)
                        .setPeriodic(INTERVAL, FLEX)
                        .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .setUpdateCurrent(true)
                        .build()
                        .schedule()

                UpdateData.instance.jobScheduleVersion = BuildConfig.VERSION_CODE
            } catch (e: Exception) {
                Log.e(LOG_TAG, "schedule(version=${BuildConfig.VERSION_CODE})", e)
            }
        }
    }

    override fun onRunJob(params: Params): Result {
        Log.d(LOG_TAG, "onRunJob(params=$params) -> Starting update check")
        return Updater.fetchUpdates().also {
            Updater.notifyAboutUpdate(context.applicationContext)
        }
    }
}
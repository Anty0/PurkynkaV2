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

package cz.anty.purkynka.update

import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import eu.codetopic.java.utils.log.Log

/**
 * @author anty
 */
class UpdateCheckJob : Job() {

    companion object {

        private const val LOG_TAG = "UpdateCheckJob"

        private const val INTERVAL = 1_000L * 60L * 60L * 6L // 6 hours
        private const val FLEX = 1_000L * 60L * 60L * 1L // 1 hour

        const val TAG = "UPDATE_CHECK"

        fun schedule(): Int = JobRequest.Builder(TAG)
                .setPeriodic(INTERVAL, FLEX)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setUpdateCurrent(true)
                .build()
                .schedule()
    }

    override fun onRunJob(params: Params): Result {
        // TODO: implement
        Log.w(LOG_TAG, "onRunJob(params=$params) -> Received request to check for update.")
        return Result.SUCCESS
    }
}
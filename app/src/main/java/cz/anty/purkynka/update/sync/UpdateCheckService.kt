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

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.evernote.android.job.Job
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.update.load.UpdateFetcher
import cz.anty.purkynka.update.notify.UpdateNotifyChannel
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.update.receiver.UpdateFetchReceiver
import cz.anty.purkynka.update.save.UpdateData
import eu.codetopic.java.utils.JavaExtensions.alsoIf
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions
import eu.codetopic.utils.AndroidExtensions.sendSuspendOrderedBroadcast
import eu.codetopic.utils.UtilsBase
import eu.codetopic.utils.notifications.manager.create.NotificationBuilder
import eu.codetopic.utils.notifications.manager.create.NotificationBuilder.Companion.requestShow

/**
 * @author anty
 */
class UpdateCheckService : IntentService(LOG_TAG) {

    companion object {

        private const val LOG_TAG = "UpdateCheckService"

        fun start(context: Context) {
            context.startService(Intent(context, UpdateCheckService::class.java))
        }

        fun requestFetchUpdates(context: Context) =
                context.sendBroadcast(UpdateFetchReceiver.getIntent(context))

        suspend fun requestSuspendFetchUpdates(context: Context): Job.Result =
                context.sendSuspendOrderedBroadcast(
                        intent = UpdateFetchReceiver.getIntent(context),
                        initialResult = AndroidExtensions.OrderedBroadcastResult(UpdateCheckJob.REQUEST_RESULT_UNKNOWN)
                ).let {
                    when (it.code) {
                        UpdateCheckJob.REQUEST_RESULT_OK ->
                            it.extras?.getSerializable(UpdateCheckJob.REQUEST_EXTRA_RESULT) as? Job.Result
                                    ?: throw RuntimeException("Failed to extract result of UpdateFetchReceiver")
                        UpdateCheckJob.REQUEST_RESULT_FAIL ->
                            throw it.extras?.getSerializable(UpdateCheckJob.REQUEST_EXTRA_THROWABLE) as? Throwable
                                    ?: RuntimeException("Unknown fail result received from UpdateFetchReceiver")
                        UpdateCheckJob.REQUEST_RESULT_UNKNOWN ->
                            throw RuntimeException("Failed to process broadcast by UpdateFetchReceiver")
                        else -> throw RuntimeException("Unknown resultCode received from UpdateFetchReceiver: ${it.code}")
                    }
                }

        fun fetchUpdates(): Job.Result {
            Log.w(LOG_TAG, "fetchUpdates(process=${UtilsBase.Process.name})")

            val code = UpdateFetcher.fetchVersionCode() ?: return Job.Result.FAILURE
            val name = UpdateFetcher.fetchVersionName() ?: return Job.Result.FAILURE

            UpdateData.instance.setLatestVersion(code, name)

            return Job.Result.SUCCESS
        }
    }

    init {
        setIntentRedelivery(true)
    }

    override fun onHandleIntent(intent: Intent?) {
        Log.w(LOG_TAG, "onHandleIntent(intent=$intent) -> Checking for update")

        val result = fetchUpdates().alsoIf({
            it == Job.Result.SUCCESS && UpdateData.instance.latestVersionCode != BuildConfig.VERSION_CODE
        }) {
            Log.w(LOG_TAG, "onHandleIntent(intent=$intent)" +
                    " -> Check for update -> Found update" +
                    " -> (${BuildConfig.VERSION_CODE} -> ${UpdateData.instance.latestVersionCode})")

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
            ).requestShow(this)
        }

        Log.w(LOG_TAG, "onHandleIntent(intent=$intent)" +
                " -> Checking for update -> Update result -> (result=$result)")
    }
}
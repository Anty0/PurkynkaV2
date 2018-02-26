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
import android.support.annotation.MainThread
import android.support.annotation.WorkerThread
import com.evernote.android.job.Job
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.update.load.UpdateFetcher
import cz.anty.purkynka.update.notify.UpdateNotifyChannel
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.update.save.UpdateData
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.UtilsBase
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.notifications.manager.create.NotificationBuilder
import eu.codetopic.utils.notifications.manager.data.NotifyId
import eu.codetopic.utils.notifications.manager.data.requestCancel
import eu.codetopic.utils.notifications.manager.requestShow
import eu.codetopic.utils.thread.LooperUtils
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.Ref
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * @author anty
 */
object Updater {

    private const val LOG_TAG = "Updater"

    @WorkerThread
    fun fetchUpdates(): Job.Result {
        Log.d(LOG_TAG, "fetchUpdates()")

        val code = UpdateFetcher.fetchVersionCode() ?: return Job.Result.FAILURE
        val name = UpdateFetcher.fetchVersionName() ?: return Job.Result.FAILURE

        UpdateData.instance.latestVersion = code to name

        return Job.Result.SUCCESS
    }

    @WorkerThread
    fun fetchFakeUpdates(): Job.Result {
        Log.d(LOG_TAG, "fetchFakeUpdates()")

        val code = BuildConfig.VERSION_CODE + 1
        val name = BuildConfig.VERSION_NAME + "-FAKE"

        UpdateData.instance.latestVersion = code to name

        return Job.Result.SUCCESS
    }

    @WorkerThread
    fun notifyAboutUpdate(appContext: Context) {
        val currentCode = BuildConfig.VERSION_CODE
        val (code, name) = UpdateData.instance.latestVersion

        if (code == currentCode) {
            LooperUtils.postOnMainThread { appContext.cancelUpdateNotification() }
        } else {
            LooperUtils.postOnMainThread { appContext.showUpdateNotification(code, name) }
        }
    }

    suspend fun suspendNotifyAboutUpdate(contextRef: Ref<Context>) = launch(UI) {
        val currentCode = BuildConfig.VERSION_CODE
        val (code, name) = bg { UpdateData.instance.latestVersion }.await()

        if (code == currentCode) {
            contextRef().cancelUpdateNotification()
        } else {
            contextRef().showUpdateNotification(code, name)
        }
    }

    @MainThread
    private fun Context.cancelUpdateNotification() {
        NotifyId.forCommon(
                groupId = UpdateNotifyGroup.ID,
                channelId = UpdateNotifyChannel.ID,
                id = UpdateNotifyChannel.NOTIFY_ID
        ).requestCancel(this)
    }

    @MainThread
    private fun Context.showUpdateNotification(versionCode: Int, versionName: String) {
        NotificationBuilder.create(
                    groupId = UpdateNotifyGroup.ID,
                    channelId = UpdateNotifyChannel.ID,
                    init = {
                        persistent = false
                        refreshable = false
                        data = UpdateNotifyChannel.dataForVersion(
                                code = versionCode,
                                name = versionName
                        )
                    }
            ).requestShow(this)
    }
}
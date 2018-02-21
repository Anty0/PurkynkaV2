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

package cz.anty.purkynka.lunches.sync

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.exceptions.LoginExpiredException
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.lunches.load.LunchesFetcher
import cz.anty.purkynka.lunches.load.LunchesParser
import cz.anty.purkynka.lunches.notify.LunchesBurzaWatcherResultChannel
import cz.anty.purkynka.lunches.notify.LunchesBurzaWatcherStatusChannel
import cz.anty.purkynka.lunches.notify.LunchesBurzaWatcherStatusGroup
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesLoginData
import eu.codetopic.java.utils.kSerializer
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.putKSerializableExtra
import eu.codetopic.utils.getKSerializableExtra
import eu.codetopic.utils.notifications.manager.build
import eu.codetopic.utils.notifications.manager.create.NotificationBuilder
import eu.codetopic.utils.notifications.manager.data.NotifyId
import eu.codetopic.utils.notifications.manager.requestShow
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.map
import org.jetbrains.anko.ctx

/**
 * @author anty
 */
class LunchesBurzaWatcherService : Service() {

    companion object {

        private const val LOG_TAG = "LunchesBurzaWatcherService"

        private const val ACTION_START_WATCHER =
                "cz.anty.purkynka.lunches.sync.$LOG_TAG.ACTION_START_WATCHER"
        private const val ACTION_STOP_WATCHER =
                "cz.anty.purkynka.lunches.sync.$LOG_TAG.ACTION_STOP_WATCHER"
        private const val ACTION_REQUEST_STATUS_UPDATE =
                "cz.anty.purkynka.lunches.sync.$LOG_TAG.ACTION_REQUEST_STATUS_UPDATE"
        private const val EXTRA_ACCOUNT_ID = "$LOG_TAG.EXTRA_ACCOUNT_ID"
        private const val EXTRA_ARGUMENTS = "$LOG_TAG.EXTRA_ARGUMENTS"

        const val ACTION_STATUS_UPDATE =
                "cz.anty.purkynka.lunches.sync.$LOG_TAG.ACTION_STATUS_UPDATE"
        const val EXTRA_STATUS_MAP = "$LOG_TAG.EXTRA_STATUS_MAP"
        val EXTRA_STATUS_MAP_SERIALIZER =
                (StringSerializer to kSerializer<BurzaWatcherStatus>()).map

        fun getStartWatcherIntent(context: Context, accountId: String,
                                  burzaWatcherArguments: BurzaWatcherArguments): Intent =
                Intent(context, LunchesBurzaWatcherService::class.java)
                        .setAction(ACTION_START_WATCHER)
                        .putExtra(EXTRA_ACCOUNT_ID, accountId)
                        .putKSerializableExtra(EXTRA_ARGUMENTS, burzaWatcherArguments)

        fun getStopWatcherIntent(context: Context, accountId: String): Intent =
                Intent(context, LunchesBurzaWatcherService::class.java)
                        .setAction(ACTION_STOP_WATCHER)
                        .putExtra(EXTRA_ACCOUNT_ID, accountId)

        fun getRequestStatusUpdateIntent(context: Context): Intent =
                Intent(context, LunchesBurzaWatcherService::class.java)
                        .setAction(ACTION_REQUEST_STATUS_UPDATE)
    }

    @Serializable
    class BurzaWatcherArguments(val targetDate: Long, val targetLunchNumbers: Array<Int>)

    @Serializable
    class BurzaWatcherStatus {

        var running: Boolean = false
        var stopping: Boolean = false
        var success: Boolean = false
        var fail: Boolean = false
        var refreshCount: Long = 0
        var orderCount: Long = 0

        var arguments: BurzaWatcherArguments? = null
    }

    private var isForeground = false
    private var lastStopRequest: Pair<String, Int>? = null
    private val status = mutableMapOf<String, BurzaWatcherStatus>()

    private fun broadcastStatusUpdate() {
        sendBroadcast(
                Intent(ACTION_STATUS_UPDATE)
                        .putKSerializableExtra(EXTRA_STATUS_MAP, status,
                                EXTRA_STATUS_MAP_SERIALIZER)
                        .also {
                            if (Build.VERSION.SDK_INT >= 16)
                                it.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        }
        )
    }

    private fun showResultNotification(accountId: String, success: Boolean) {
        NotificationBuilder.create(
                groupId = AccountNotifyGroup.idFor(accountId),
                channelId = LunchesBurzaWatcherResultChannel.ID
        ) {
            persistent = true
            refreshable = true
            data = LunchesBurzaWatcherResultChannel.dataFor(success)
        }.requestShow(this)
    }

    private fun buildForegroundNotification(): Pair<NotifyId, Notification> =
            NotificationBuilder
                    .create(
                            groupId = LunchesBurzaWatcherStatusGroup.ID,
                            channelId = LunchesBurzaWatcherStatusChannel.ID
                    )
                    .build(this, false)

    private fun checkStartForeground() {
        if (isForeground) return

        isForeground = true
        val (notifyId, notification) = buildForegroundNotification()
        startForeground(notifyId.idNotify, notification)
    }

    private fun checkStopForeground() {
        if (!isForeground) return

        // Is running?
        if (status.any { it.value.running }) return

        isForeground = false
        stopForeground(true)

    }

    private fun startWatcher(accountId: String) {
        val status = status[accountId] ?: run {
            Log.e(LOG_TAG, "startWatcher(accountId=$accountId)" +
                    " -> Failed to start burza watcher:" +
                    " Burza watcher status for this account not found")
            checkStopForeground()
            return
        }

        val arguments = status.arguments ?: run {
            Log.e(LOG_TAG, "startWatcher(accountId=$accountId)" +
                    " -> Failed to start burza watcher:" +
                    " Burza watcher arguments for this account not found")
            checkStopForeground()
            return
        }

        val targetDate = arguments.targetDate
        val targetLunchNumbers = arguments.targetLunchNumbers

        launch watcherLoop@ {
            try {
                val loginData = LunchesLoginData.loginData

                if (!loginData.isLoggedIn(accountId))
                    throw IllegalStateException("User is not logged in")

                val (username, password) = loginData.getCredentials(accountId)

                if (username == null || password == null)
                    throw IllegalStateException("Username or password is null")

                var cookies = LunchesFetcher.login(username, password)

                if (!LunchesFetcher.isLoggedIn(cookies))
                    throw WrongLoginDataException("Failed to login user with provided credentials")

                var failCount = 0
                while (status.running && !status.stopping) {
                    try {
                        run checkBurza@ {
                            val burzaPge = LunchesFetcher.getBurzaPage(cookies)
                            if (!LunchesFetcher.isLoggedIn(burzaPge)) {
                                LunchesFetcher.logout(cookies)

                                throw LoginExpiredException()
                            }
                            val burzaLunchesHtml = LunchesFetcher.getLunchesBurzaElements(burzaPge)
                            val burzaLunchesList = LunchesParser.parseLunchesBurza(burzaLunchesHtml)

                            val selectedLunch = burzaLunchesList
                                    .firstOrNull {
                                        it.date == targetDate &&
                                                it.lunchNumber in targetLunchNumbers
                                    }
                                    ?: return@checkBurza

                            status.orderCount++

                            LunchesFetcher.orderLunch(cookies, selectedLunch.orderUrl)

                            val lunchHtml = LunchesFetcher.getLunchOptionsGroupElement(cookies, selectedLunch.date)
                            val lunch = LunchesParser.parseLunchOptionsGroup(lunchHtml)

                            lunch.orderedOption ?: return@checkBurza

                            status.success = true
                            status.stopping = true

                            LunchesData.instance.invalidateData(accountId)

                            launch(UI) { LunchesSyncAdapter.requestSync(Accounts.get(ctx, accountId)) }
                        }

                        status.refreshCount++

                        launch(UI) { broadcastStatusUpdate() }.join()
                        // wait for broadcastStatusUpdate() complete, to give cpu some free time
                        // (and protect app against stacking launch() requests).

                        if (failCount > 10)
                            failCount -= 10
                        else failCount = 0

                        status.fail = failCount > 10
                    } catch (e: Exception) {
                        // Due to loop this catch can receive spam of exceptions.
                        // So this exception will be logged as debug,
                        //  to protect app from issues spam.
                        Log.d(LOG_TAG, "startWatcher(accountId=$accountId)", e)

                        if (e is LoginExpiredException) {
                            cookies = LunchesFetcher.login(username, password)
                            if (!LunchesFetcher.isLoggedIn(cookies))
                                throw WrongLoginDataException("Failed to login user with provided credentials")
                        }

                        failCount++
                        if (failCount > 100) failCount = 100
                    }
                }

                LunchesFetcher.logout(cookies)
            } catch (e: Exception) {
                Log.w(LOG_TAG, "startWatcher(accountId=$accountId)", e)

                status.fail = true
            }

            val stopped = status.stopping

            status.running = false
            status.stopping = false

            launch(UI) finalizer@ {
                if (!stopped) showResultNotification(accountId, status.success)

                broadcastStatusUpdate()

                checkStopForeground()

                if (isForeground) return@finalizer
                val (lastAccountId, startCode) = lastStopRequest ?: return@finalizer
                if (lastAccountId != accountId) return@finalizer
                stopSelf(startCode)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        run processIntent@ {
            when (intent?.action) {
                ACTION_START_WATCHER -> {
                    val accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
                            ?: throw IllegalArgumentException("AccountId not received")

                    val arguments = intent
                            .getKSerializableExtra<BurzaWatcherArguments>(EXTRA_ARGUMENTS)
                            ?: throw IllegalArgumentException("BurzaWatcherArguments not received")

                    status.getOrPut(accountId) { BurzaWatcherStatus() }
                            .takeUnless { it.running }
                            ?.also {
                                it.running = true
                                it.stopping = false
                                it.fail = false
                                it.arguments = arguments
                                it.refreshCount = 0
                                it.orderCount = 0
                            }
                            ?: return@processIntent

                    checkStartForeground()

                    broadcastStatusUpdate()

                    startWatcher(accountId)
                }
                ACTION_STOP_WATCHER -> {
                    val accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
                            ?: throw IllegalArgumentException("AccountId not received")


                    val accountStatus = status.getOrPut(accountId) { BurzaWatcherStatus() }

                    if (!accountStatus.running || accountStatus.stopping) return@processIntent
                    lastStopRequest = accountId to startId
                    accountStatus.stopping = true

                    broadcastStatusUpdate()
                }
                ACTION_REQUEST_STATUS_UPDATE -> {
                    broadcastStatusUpdate()
                    if (!isForeground) stopSelf(startId)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        status.filter { it.value.running }
                .forEach { it.value.stopping = true }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
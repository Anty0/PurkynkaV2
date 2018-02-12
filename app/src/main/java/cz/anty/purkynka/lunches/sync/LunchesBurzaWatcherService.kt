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

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.lunches.load.LunchesFetcher
import cz.anty.purkynka.lunches.load.LunchesParser
import cz.anty.purkynka.lunches.notify.LunchesBurzaWatcherStatusChannel
import cz.anty.purkynka.lunches.notify.LunchesBurzaWatcherStatusGroup
import cz.anty.purkynka.lunches.save.LunchesLoginData
import eu.codetopic.java.utils.JavaExtensions.kSerializer
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.putKSerializableExtra
import eu.codetopic.utils.AndroidExtensions.getKSerializableExtra
import eu.codetopic.utils.notifications.manager.create.NotificationBuilder
import eu.codetopic.utils.notifications.manager.create.NotificationBuilder.Companion.build
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.map
import org.jetbrains.anko.coroutines.experimental.bg

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
    class BurzaWatcherArguments(val repeatedFailureCancel: Boolean,
                                val targetDate: Long, val targetLunchNumber: Int)

    @Serializable
    class BurzaWatcherStatus {

        var running: Boolean = false
        var stopping: Boolean = false
        var success: Boolean = false
        var fail: Boolean = false

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
        )
    }

    private fun checkStartForeground() {
        if (!isForeground) {
            val (notifyId, notification) = NotificationBuilder
                    .create(
                            groupId = LunchesBurzaWatcherStatusGroup.ID,
                            channelId = LunchesBurzaWatcherStatusChannel.ID
                    )
                    .build(this, false)
            startForeground(notifyId.idNotify, notification)
        }
    }

    private fun checkStopForeground() {
        if (isForeground && !status.any { it.value.running }) {
            isForeground = false
            stopForeground(true)
        }
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

        val countFails = arguments.repeatedFailureCancel
        val targetDate = arguments.targetDate
        val targetLunchNumber = arguments.targetLunchNumber

        bg {
            try {
                val loginData = LunchesLoginData.loginData

                if (!loginData.isLoggedIn(accountId))
                    throw IllegalStateException("User is not logged in")

                val (username, password) = loginData.getCredentials(accountId)

                if (username == null || password == null)
                    throw IllegalStateException("Username or password is null")

                val cookies = LunchesFetcher.login(username, password)

                if (!LunchesFetcher.isLoggedIn(cookies))
                    throw WrongLoginDataException("Failed to login user with provided credentials")

                var failCount = 0
                while (status.running && !status.stopping) {
                    try {
                        val burzaLunchesHtml = LunchesFetcher.getBurzaLunchesElements(cookies)
                        val burzaLunchesList = LunchesParser.parseBurzaLunches(burzaLunchesHtml)

                        val selectedLunch = burzaLunchesList
                                .firstOrNull {
                                    it.date == targetDate &&
                                            it.lunchNumber == targetLunchNumber
                                }
                                ?: continue

                        LunchesFetcher.orderLunch(cookies, selectedLunch.orderUrl)

                        val lunchHtml = LunchesFetcher.getLunchOptionsGroupElement(cookies, selectedLunch.date)
                        val lunch = LunchesParser.parseLunchOptionsGroup(lunchHtml)

                        if (lunch.orderedOption != null) {
                            status.success = true
                            // TODO: launch(UI) { successNotification() }
                            break
                        }

                        if (countFails) failCount = 0
                    } catch (e: Exception) {
                        Log.w(LOG_TAG, "startWatcher(accountId=$accountId)", e)

                        if (countFails && ++failCount > 10) {
                            status.fail = true
                            break
                        }
                    }
                }

                LunchesFetcher.logout(cookies)
            } catch (e: Exception) {
                Log.w(LOG_TAG, "startWatcher(accountId=$accountId)", e)

                status.fail = true
            }

            status.running = false
            status.stopping = false

            launch(UI) {
                broadcastStatusUpdate()

                checkStopForeground()

                if (isForeground) return@launch
                val (lastAccountId, startCode) = lastStopRequest ?: return@launch
                if (lastAccountId != accountId) return@launch
                stopSelf(startCode)
            }

            return@bg
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
                                it.arguments = arguments
                                it.fail = false
                                it.stopping = false
                                it.running = true
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
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

import android.accounts.Account
import android.content.Context
import android.content.SyncResult
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import cz.anty.purkynka.lunches.load.LunchesFetcher
import cz.anty.purkynka.lunches.load.LunchesParser
import cz.anty.purkynka.lunches.notify.LunchesChangesNotifyChannel
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.lunches.save.LunchesPreferences
import cz.anty.purkynka.lunches.widget.NextLunchWidgetProvider
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.notifications.manager.create.MultiNotificationBuilder
import eu.codetopic.utils.notifications.manager.showAll
import java.io.IOException

/**
 * @author anty
 */
object LunchesSyncer {

    private const val LOG_TAG = "LunchesSyncer"

    fun moveLunchToOrFromBurza(
            accountId: String,
            lunchOptionsGroup: LunchOptionsGroup,
            lunchOptionToOrderIndex: Int,
            credentials: Pair<String, String>? = null
    ) {
        val lunchOptionToBurza = lunchOptionsGroup.options?.get(lunchOptionToOrderIndex)
                ?: throw IllegalArgumentException("Invalid lunchOptionToOrderIndex")

        val loginData = LunchesLoginData.loginData

        if (credentials == null && !loginData.isLoggedIn(accountId))
            throw IllegalStateException("User is not logged in")

        val (username, password) = credentials ?: loginData.getCredentials(accountId)

        if (username == null || password == null)
            throw IllegalStateException("Username or password is null")

        val cookies = LunchesFetcher.login(username, password)

        if (!LunchesFetcher.isLoggedIn(cookies))
            throw WrongLoginDataException("Failed to login user with provided credentials")

        val lunchHtml = LunchesFetcher.getLunchOptionsGroupElement(cookies, lunchOptionsGroup.date)
        val nLunchOptionsGroup = LunchesParser.parseLunchOptionsGroup(lunchHtml)
                .takeIf { it == lunchOptionsGroup }
                ?: throw IllegalStateException("Lunch options group to order not found")
        val nLunchOption = nLunchOptionsGroup.options?.get(lunchOptionToOrderIndex)
                .takeIf { it == lunchOptionToBurza && it.isInBurza == lunchOptionToBurza.isInBurza }
                ?: throw IllegalStateException("Lunch option to order not found")

        if (nLunchOptionsGroup.orderedOption != lunchOptionsGroup.orderedOption)
            throw IllegalStateException("Lunch options group was modified.")

        val url = nLunchOption.toOrFromBurzaUrl
                ?: throw IllegalStateException("Lunch option url to order not found")

        LunchesFetcher.orderLunch(cookies, url)

        LunchesFetcher.logout(cookies)

        LunchesData.instance.invalidateData(accountId)
    }

    fun orderLunch(
            accountId: String,
            lunchOptionsGroup: LunchOptionsGroup,
            lunchOptionToOrderIndex: Int,
            credentials: Pair<String, String>? = null
    ) {
        val lunchOptionToOrder = lunchOptionsGroup.options?.get(lunchOptionToOrderIndex)
                ?: throw IllegalArgumentException("Invalid lunchOptionToOrderIndex")

        val loginData = LunchesLoginData.loginData

        if (credentials == null && !loginData.isLoggedIn(accountId))
            throw IllegalStateException("User is not logged in")

        val (username, password) = credentials ?: loginData.getCredentials(accountId)

        if (username == null || password == null)
            throw IllegalStateException("Username or password is null")

        val cookies = LunchesFetcher.login(username, password)

        if (!LunchesFetcher.isLoggedIn(cookies))
            throw WrongLoginDataException("Failed to login user with provided credentials")

        val lunchHtml = LunchesFetcher.getLunchOptionsGroupElement(cookies, lunchOptionsGroup.date)
        val nLunchOptionsGroup = LunchesParser.parseLunchOptionsGroup(lunchHtml)
                .takeIf { it == lunchOptionsGroup }
                ?: throw IllegalStateException("Lunch options group to order not found")
        val nLunchOptionToOrder = nLunchOptionsGroup.options?.get(lunchOptionToOrderIndex)
                .takeIf {
                    it == lunchOptionToOrder
                            && it.enabled == lunchOptionToOrder.enabled
                            && it.ordered == lunchOptionToOrder.ordered
                }
                ?: throw IllegalStateException("Lunch option to order not found")

        if (nLunchOptionsGroup.orderedOption != lunchOptionsGroup.orderedOption)
            throw IllegalStateException("Lunch options group was modified.")

        val url = nLunchOptionToOrder.orderOrCancelUrl
                ?: throw IllegalStateException("Lunch option url to order not found")

        LunchesFetcher.orderLunch(cookies, url)

        LunchesFetcher.logout(cookies)

        LunchesData.instance.invalidateData(accountId)
    }

    fun performSync(
            context: Context,
            account: Account,
            credentials: Pair<String, String>? = null,
            firstSync: Boolean = false,
            syncResult: SyncResult? = null
    ) {
        var data: LunchesData? = null
        var preferences: LunchesPreferences? = null

        var accountId: String? = null

        try {
            data = LunchesData.instance
            val loginData = LunchesLoginData.loginData
            preferences = LunchesPreferences.instance

            accountId = Accounts.getId(context, account)

            if (credentials == null && !loginData.isLoggedIn(accountId))
                throw IllegalStateException("User is not logged in")

            val (username, password) = credentials ?: loginData.getCredentials(accountId)

            if (username == null || password == null)
                throw IllegalStateException("Username or password is null")

            val cookies = LunchesFetcher.login(username, password)

            val mainPage = LunchesFetcher.getMainPage(cookies)

            if (!LunchesFetcher.isLoggedIn(mainPage))
                throw WrongLoginDataException("Failed to login user with provided credentials")

            val nLunchesHtml = LunchesFetcher.getLunchOptionsGroupsElements(cookies)

            LunchesFetcher.logout(cookies)

            val lunchesList = data.getLunches(accountId)

            val nCredit = try {
                LunchesParser.parseCredit(mainPage)
            } catch (e: Exception) {
                if (e is InterruptedException) throw e

                // Problems with parsing credit shouldn't cause whole sync to fail,
                //  but system will be notified about result
                syncResult?.apply { stats.numParseExceptions++ }
                Log.w(LOG_TAG, "Failed to parse lunches credit", e)
                Float.NaN
            }
            val nLunchesList = LunchesParser.parseLunchOptionsGroups(nLunchesHtml, syncResult)

            if (!firstSync) {
                checkForDifferences(context, accountId, lunchesList, nLunchesList, syncResult)
            } else {
                syncResult?.apply {
                    stats.numDeletes += lunchesList.count()
                    stats.numInserts += nLunchesList.count()
                }
            }

            data.setCredit(accountId, nCredit)
            data.setLunches(accountId, nLunchesList)
            data.makeDataValid(accountId)

            data.setLastSyncResult(accountId, LunchesData.SyncResult.SUCCESS)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to refresh lunches", e)

            if (data != null && accountId != null) run setResult@ {
                data.setLastSyncResult(accountId, when (e) {
                    is WrongLoginDataException -> {
                        syncResult?.apply { stats.numAuthExceptions++ }
                        LunchesData.SyncResult.FAIL_LOGIN
                    }
                    is IOException -> {
                        syncResult?.apply { stats.numIoExceptions++ }
                        LunchesData.SyncResult.FAIL_CONNECT
                    }
                    is InterruptedException -> return@setResult
                    else -> {
                        syncResult?.apply { stats.numIoExceptions++ }
                        LunchesData.SyncResult.FAIL_UNKNOWN
                    }
                })
            }

            throw e
        } finally {
            try {
                if (preferences != null) {
                    context.sendBroadcast(
                            NextLunchWidgetProvider.getUpdateIntent(
                                    context,
                                    NextLunchWidgetProvider.getAllWidgetIds(context)
                                            .filter { preferences.getAppWidgetAccountId(it) == accountId }
                                            .toIntArray()
                            )
                    )
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to refresh next lunch widgets", e)
            }
        }
    }

    private fun checkForDifferences(
            context: Context,
            accountId: String,
            oldLunches: List<LunchOptionsGroup>,
            newLunches:  List<LunchOptionsGroup>,
            syncResult: SyncResult?
    ) {
        val inserts = newLunches.toMutableList()
        val removes = mutableListOf<LunchOptionsGroup>()
        val changes = mutableListOf<LunchOptionsGroup>()

        val interestingChanges = mutableListOf<LunchOptionsGroup>()

        oldLunches.forEach {
            val newIndex = inserts.indexOf(it)
            if (newIndex == -1) {
                syncResult?.apply { stats.numDeletes++ }
                removes.add(it)
                return@forEach
            }

            val newIt = inserts.removeAt(newIndex)
            if (it isDifferentFrom newIt) {
                syncResult?.apply { stats.numUpdates++ }
                changes.add(newIt)

                val oldHadNoUsableOptions = it.options == null ||
                        (it.orderedOption == null && it.options.all { !it.enabled })
                if (oldHadNoUsableOptions) {
                    val newHaveUsableOptions = newIt.options != null
                            && newIt.options.any { it.enabled }
                    if (newHaveUsableOptions) {
                        Log.b(LOG_TAG, "checkForDifferences()" +
                                " -> Found interesting change" +
                                " -> (oldLunch=$it, newLunch=$newIt)")
                        interestingChanges.add(newIt)
                    }
                }
            }
        }

        syncResult?.apply { stats.numInserts += inserts.count() }

        interestingChanges.addAll(
                inserts.filter {
                    it.options != null && it.options.any { it.enabled }
                }
        )

        if (interestingChanges.isEmpty()) return

        MultiNotificationBuilder.create(
                groupId = AccountNotifyGroup.idFor(accountId),
                channelId = LunchesChangesNotifyChannel.ID
        ) {
            persistent = true
            refreshable = true
            data = interestingChanges.map {
                LunchesChangesNotifyChannel.dataFor(it)
            }
        }.showAll(context)
    }
}
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
import android.content.*
import android.os.Bundle
import cz.anty.purkynka.utils.*
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.account.Syncs
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import cz.anty.purkynka.lunches.load.LunchesFetcher
import cz.anty.purkynka.lunches.load.LunchesParser
import cz.anty.purkynka.lunches.notify.LunchesChangesNotifyChannel
import cz.anty.purkynka.lunches.receiver.UpdateLunchesSyncReceiver
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesData.SyncResult.*
import cz.anty.purkynka.lunches.save.LunchesDataProvider
import cz.anty.purkynka.lunches.save.LunchesLoginData
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.broadcast.BroadcastsConnector
import eu.codetopic.utils.notifications.manager.create.MultiNotificationBuilder
import eu.codetopic.utils.notifications.manager.create.MultiNotificationBuilder.Companion.requestShowAll
import java.io.IOException

/**
 * @author anty
 */
class LunchesSyncAdapter(context: Context) :
        AbstractThreadedSyncAdapter(context, false, false) {

    companion object {

        private const val LOG_TAG = "LunchesSyncAdapter"

        const val CONTENT_AUTHORITY = LunchesDataProvider.AUTHORITY
        const val SYNC_FREQUENCY = SYNC_FREQUENCY_LUNCHES

        fun init(context: Context) {
            BroadcastsConnector.connect(
                    LunchesLoginData.instance.broadcastActionChanged,
                    BroadcastsConnector.Connection(
                            BroadcastsConnector.BroadcastTargetingType.GLOBAL,
                            UpdateLunchesSyncReceiver.getIntent(context)
                    )
            )
            context.sendBroadcast(UpdateLunchesSyncReceiver.getIntent(context))
        }

        fun updateSyncable(context: Context) {
            Syncs.updateAllAccountsBasedEnabled(
                    context = context,
                    loginData = LunchesLoginData.loginData,
                    contentAuthority = CONTENT_AUTHORITY,
                    syncFrequency = SYNC_FREQUENCY
            )
        }

        fun updateSyncableOf(accountId: String, account: Account) {
            Syncs.updateAccountBasedEnabled(
                    accountId = accountId,
                    account = account,
                    loginData = LunchesLoginData.loginData,
                    contentAuthority = CONTENT_AUTHORITY,
                    syncFrequency = SYNC_FREQUENCY
            )
        }

        fun requestSync(account: Account) {
            Log.d(LOG_TAG, "requestSync(account=$account)")

            Syncs.trigger(
                    account = account,
                    contentAuthority = CONTENT_AUTHORITY
            )
        }
    }


    override fun onPerformSync(account: Account, extras: Bundle, authority: String,
                               provider: ContentProviderClient, syncResult: SyncResult) {
        Log.d(LOG_TAG, "onPerformSync(account=(type=${account.type}," +
                " name=${account.name}), authority=$authority)")

        if (authority != CONTENT_AUTHORITY) return

        val data = LunchesData.instance
        val loginData = LunchesLoginData.loginData

        val accountId = Accounts.getId(context, account)

        try {
            val firstSync = data.isFirstSync(accountId)

            if (!loginData.isLoggedIn(accountId))
                throw IllegalStateException("User is not logged in")

            val (username, password) = loginData.getCredentials(accountId)

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
                // Problems with parsing credit shouldn't cause whole sync to fail,
                //  but system will be notified about result
                syncResult.stats.numParseExceptions++
                Log.w(LOG_TAG, "Failed to parse lunches credit", e)
                Float.NaN
            }
            val nLunchesList = LunchesParser.parseLunchOptionsGroups(nLunchesHtml, syncResult)

            if (!firstSync) {
                checkForDifferences(accountId, lunchesList, nLunchesList, syncResult)
            } else {
                syncResult.stats.numDeletes += lunchesList.count()
                syncResult.stats.numInserts += nLunchesList.count()
            }

            data.setCredit(accountId, nCredit)
            data.setLunches(accountId, nLunchesList)
            data.notifyFirstSyncDone(accountId)
            data.makeDataValid(accountId)
            data.setLastSyncResult(accountId, SUCCESS)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to refresh lunches", e)

            data.setLastSyncResult(accountId, when (e) {
                is WrongLoginDataException -> {
                    syncResult.stats.numAuthExceptions++
                    FAIL_LOGIN
                }
                is IOException -> {
                    syncResult.stats.numIoExceptions++
                    FAIL_CONNECT
                }
                else -> {
                    syncResult.stats.numIoExceptions++
                    FAIL_UNKNOWN
                }
            })
        }
    }

    fun checkForDifferences(accountId: String,
                            oldLunches: List<LunchOptionsGroup>,
                            newLunches:  List<LunchOptionsGroup>,
                            syncResult: SyncResult) {
        val inserts = newLunches.toMutableList()
        val removes = mutableListOf<LunchOptionsGroup>()
        val changes = mutableListOf<LunchOptionsGroup>()

        val interestingChanges = mutableListOf<LunchOptionsGroup>()

        oldLunches.forEach {
            val newIndex = inserts.indexOf(it)
            if (newIndex == -1) {
                syncResult.stats.numDeletes++
                removes.add(it)
                return@forEach
            }

            val newIt = inserts.removeAt(newIndex)
            if (it isDifferentFrom newIt) {
                syncResult.stats.numUpdates++
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

        syncResult.stats.numInserts += inserts.count()

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
        }.requestShowAll(context)
    }
}
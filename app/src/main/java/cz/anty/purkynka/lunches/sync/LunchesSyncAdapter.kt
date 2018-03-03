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
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import cz.anty.purkynka.account.Syncs
import cz.anty.purkynka.lunches.receiver.UpdateLunchesSyncReceiver
import cz.anty.purkynka.lunches.save.LunchesDataProvider
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.utils.SYNC_FREQUENCY_LUNCHES
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.broadcast.BroadcastsConnector

/**
 * @author anty
 */
class LunchesSyncAdapter(context: Context) :
        AbstractThreadedSyncAdapter(context, false, false) {

    companion object {

        private const val LOG_TAG = "LunchesSyncAdapter"

        const val CONTENT_AUTHORITY = LunchesDataProvider.AUTHORITY
        const val SYNC_FREQUENCY = SYNC_FREQUENCY_LUNCHES

        fun listenForChanges(context: Context) {
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

        try {
            LunchesSyncer.performSync(
                    context = context,
                    account = account,
                    syncResult = syncResult
            )
        } catch (_: Exception) {
            // Exception handling is done in LunchesSyncer
        }
    }
}
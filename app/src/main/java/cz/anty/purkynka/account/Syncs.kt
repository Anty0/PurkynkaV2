/*
 * app
 * Copyright (C)   2017  anty
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

package cz.anty.purkynka.account

import android.accounts.Account
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.accountManager
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.data.getter.DataGetter
import eu.codetopic.utils.data.preferences.extension.LoginDataExtension
import org.jetbrains.anko.bundleOf

/**
 * @author anty
 */
object Syncs {

    fun trigger(account: Account, contentAuthority: String, extras: Bundle? = null) {
        ContentResolver.requestSync(
                account,
                contentAuthority,
                bundleOf(
                        ContentResolver.SYNC_EXTRAS_MANUAL to true,
                        ContentResolver.SYNC_EXTRAS_EXPEDITED to true
                ).apply { extras?.let { putAll(it) } }
        )
    }

    fun updateEnabled(enable: Boolean, account: Account, contentAuthority: String,
                     syncFrequency: Long, extras: Bundle = Bundle()) {
        ContentResolver.setIsSyncable(account, contentAuthority, if (enable) 1 else 0)
        ContentResolver.setSyncAutomatically(account, contentAuthority, enable)
        if (enable) ContentResolver.addPeriodicSync(account, contentAuthority, extras, syncFrequency)
        else ContentResolver.removePeriodicSync(account, contentAuthority, extras)
    }

    fun updateAllAccountsBasedEnabled(context: Context,
                                      loginData: LoginDataExtension<*>,
                                      contentAuthority: String, syncFrequency: Long) {
        Accounts.getAllWIthIds(context).forEach {
            val (accountId, account) = it

            updateAccountBasedEnabled(accountId, account,
                    loginData, contentAuthority, syncFrequency)
        }
    }

    fun updateAccountBasedEnabled(accountId: String, account: Account,
                                  loginData: LoginDataExtension<*>,
                                  contentAuthority: String, syncFrequency: Long) {
        val loggedIn = loginData.isLoggedIn(accountId)
        /*val syncable = ContentResolver.getIsSyncable(account, contentAuthority)
                .takeIf { it >= 0 }?.let { it > 0 }
        if (syncable != null && loggedIn == syncable) return@forEach

        Log.d(logTag, "loginDataChanged() -> differenceFound(account=$account," +
                " loggedIn=$loggedIn, syncable=${syncable ?: "unknown"})")*/

        updateEnabled(
                enable = loggedIn,
                account = account,
                contentAuthority = contentAuthority,
                syncFrequency = syncFrequency
        )
    }
}
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

package cz.anty.purkynka.account.save

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.utils.ACTIVE_ACCOUNT_ID
import eu.codetopic.java.utils.asPair
import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.support.ContentProviderSharedPreferences
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs
import org.jetbrains.anko.accountManager

/**
 * @author anty
 */
class ActiveAccount private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, ActiveAccountProvider.AUTHORITY)) {

    companion object :
            PreferencesCompanionObject<ActiveAccount>(
                    ActiveAccount.LOG_TAG,
                    ::ActiveAccount,
                    ::Getter
            ) {

        private const val LOG_TAG = "ActiveAccount"
        internal const val SAVE_VERSION = 0

        @Suppress("UNUSED_PARAMETER")
        internal fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
            // This function will be executed by provider in provider process
            when (from) {
                -1 -> {
                    // First start, nothing to do
                }
            } // No more versions yet
        }

        fun getWithId(): Pair<String?, Account?> =
                instance.accountWithId

        fun set(accountId: String?) {
            instance.accountId = accountId
        }
    }

    private val accountManager: AccountManager = context.accountManager

    private fun getActiveAccountWithId(accounts: Map<String, Account>): Pair<String, Account>? {
        val id = preferences.getString(ACTIVE_ACCOUNT_ID, null) ?: return null

        return accounts.entries
                .firstOrNull { it.key == id }
                ?.asPair()
    }

    var accountWithId: Pair<String?, Account?>
        get() = Accounts.getAllWIthIds(accountManager)
                .let { accounts ->
                    getActiveAccountWithId(accounts)
                            ?: accounts.entries.let {
                                it.firstOrNull()?.asPair()?.also {
                                    accountId = it.first
                                }
                            }
                            ?: null to null
                }
        @Deprecated("Use accountId instead", ReplaceWith("accountId"))
        set(value) { accountId = value.first }

    val account: Account?
        @Deprecated("Use accountWithId instead", ReplaceWith("accountWithId"))
        get() = accountWithId.second

    var accountId: String?
        @Deprecated("Use accountWithId instead", ReplaceWith("accountWithId"))
        get() = accountWithId.first
        set(value) = edit {
            putString(ACTIVE_ACCOUNT_ID, value)
        }

    private class Getter : PreferencesGetterAbs<ActiveAccount>() {

        override fun get() = instance

        override val dataClass: Class<out ActiveAccount>
            get() = ActiveAccount::class.java
    }
}
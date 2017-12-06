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

package cz.anty.purkynka.accounts

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build

import eu.codetopic.utils.data.preferences.VersionedPreferencesData
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs

import cz.anty.purkynka.PrefNames.*
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject

/**
 * @author anty
 */
class ActiveAccountManager private constructor(context: Context) :
        VersionedPreferencesData<SharedPreferences>(context,
                BasicSharedPreferencesProvider(context, FILE_NAME_ACTIVE_ACCOUNT_DATA),
                SAVE_VERSION) {

    companion object : PreferencesCompanionObject<ActiveAccountManager>(ActiveAccountManager.LOG_TAG, ::ActiveAccountManager, ::Getter) {

        private const val LOG_TAG = "ActiveAccountManager"
        private const val SAVE_VERSION = 0
    }

    private val accountManager: AccountManager = AccountManager.get(context)

    var activeAccount: Account?
        get() {
            val avAccounts = AccountsHelper.getAllAccounts(accountManager)
            val getFirstAccount: () -> Account? = {
                if (avAccounts.isNotEmpty()) {
                    activeAccount = avAccounts[0]
                    avAccounts[0]
                } else null
            }
            val name = preferences.getString(ACTIVE_ACCOUNT_NAME, null) ?: return getFirstAccount()

            avAccounts.filter { name == it.name }.forEach { return it }
            if (Build.VERSION.SDK_INT >= 21) {
                avAccounts.filter { name == accountManager.getPreviousName(it) }.forEach {
                    setActiveAccount(it.name)
                    return it
                }
            }

            return getFirstAccount()
        }
        set(account) = setActiveAccount(account?.name)

    val activeAccountId: String? get() {
        return AccountsHelper.getAccountId(context, activeAccount ?: return null)
    }

    fun setActiveAccount(accountName: String?) {
        edit { putString(ACTIVE_ACCOUNT_NAME, accountName) }
    }

    @Synchronized
    override fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
        when (from) {
            -1 -> {
                // First start, nothing to do
            } // No more versions yet
        }
    }

    private class Getter : PreferencesGetterAbs<ActiveAccountManager>() {

        override fun get() = instance

        override val dataClass: Class<out ActiveAccountManager>
            get() = ActiveAccountManager::class.java
    }
}

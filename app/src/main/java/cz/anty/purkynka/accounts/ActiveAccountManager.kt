/*
 * Copyright 2017 Jiří Kuchyňka (Anty)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package cz.anty.purkynka.accounts

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build

import eu.codetopic.java.utils.Objects
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

        private val LOG_TAG = "ActiveAccountManager"
        private val SAVE_VERSION = 0
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

        override fun get(): ActiveAccountManager? {
            return instance
        }

        override fun getDataClass(): Class<ActiveAccountManager> {
            return ActiveAccountManager::class.java
        }
    }
}

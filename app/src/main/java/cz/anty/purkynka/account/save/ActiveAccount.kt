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
import android.os.Build
import cz.anty.purkynka.utils.*
import cz.anty.purkynka.account.Accounts
import eu.codetopic.utils.accountManager
import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.support.ContentProviderSharedPreferences
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs

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

        fun get(): Account? =
                instance.activeAccount

        fun getWithId(): Pair<Account?, String?> =
                instance.activeAccountWithId

        fun getId(): String? =
                instance.activeAccountId

        fun set(account: Account?) {
            instance.activeAccount = account
        }

        fun set(accountName: String?) {
            instance.setActiveAccount(accountName)
        }
    }

    private val accountManager: AccountManager = context.accountManager

    var activeAccount: Account? // TODO: Maybe use account id for saving which account is active
        get() {
            val avAccounts = Accounts.getAll(accountManager)
            val useFirstAccount: () -> Account? = {
                if (avAccounts.isNotEmpty()) {
                    activeAccount = avAccounts[0]
                    avAccounts[0]
                } else null
            }
            val name = preferences.getString(ACTIVE_ACCOUNT_NAME, null)
                    ?: return run(useFirstAccount)

            avAccounts.firstOrNull { name == it.name }
                    ?.also { return it }
                    ?:
                    run {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            avAccounts.firstOrNull { name == accountManager.getPreviousName(it) }
                                    ?.also {
                                        setActiveAccount(it.name)
                                        return it
                                    }
                        }
                    }

            return run(useFirstAccount)
        }
        set(account) = setActiveAccount(account?.name)

    val activeAccountId: String?
        get() = activeAccount?.let { Accounts.getId(accountManager, it) }

    val activeAccountWithId : Pair<Account?, String?>
        get() = activeAccount.let { it to it?.let { Accounts.getId(accountManager, it) } }

    fun setActiveAccount(accountName: String?) {
        edit { putString(ACTIVE_ACCOUNT_NAME, accountName) }
    }

    private class Getter : PreferencesGetterAbs<ActiveAccount>() {

        override fun get() = instance

        override val dataClass: Class<out ActiveAccount>
            get() = ActiveAccount::class.java
    }
}
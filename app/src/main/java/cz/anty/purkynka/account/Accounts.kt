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
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Build
import cz.anty.purkynka.account.notify.AccountNotifyChannel
import eu.codetopic.utils.broadcast.BroadcastsConnector
import eu.codetopic.utils.broadcast.BroadcastsConnector.Connection
import eu.codetopic.utils.broadcast.BroadcastsConnector.BroadcastTargetingType
import eu.codetopic.utils.broadcast.LocalBroadcast
import org.jetbrains.anko.bundleOf
import java.util.*

/**
 * @author anty
 */
object Accounts {

    const val ACCOUNT_TYPE = "cz.anty.purkynka.account"

    const val ACTION_ACCOUNT_ADDED = "cz.anty.purkynka.account.ACTION_ACCOUNT_ADDED"
    const val ACTION_ACCOUNT_REMOVED = "cz.anty.purkynka.account.ACTION_ACCOUNT_REMOVED"
    const val ACTION_ACCOUNT_RENAMED = "cz.anty.purkynka.account.ACTION_ACCOUNT_RENAMED"
    const val ACTION_ACCOUNTS_CHANGED = "cz.anty.purkynka.account.ACTION_ACCOUNTS_CHANGED"

    const val EXTRA_OLD_ACCOUNT = "cz.anty.purkynka.account.EXTRA_OLD_ACCOUNT"
    const val EXTRA_ACCOUNT = "cz.anty.purkynka.account.EXTRA_ACCOUNT"
    const val EXTRA_ACCOUNT_ID = "cz.anty.purkynka.account.EXTRA_ACCOUNT_ID"

    private const val KEY_ACCOUNT_ID = "cz.anty.purkynka.account.ACCOUNT_ID"

    var isInitialized = false
        private set

    fun initialize(context: Context) {
        initBroadcastConnections()
        initDefaultAccount(context)
        ActiveAccount.initialize(context)
        AccountNotifyChannel.init(context)
        isInitialized = true
    }

    private fun initBroadcastConnections() {
        val connection = Connection(
                BroadcastTargetingType.LOCAL,
                ACTION_ACCOUNTS_CHANGED
        )
        arrayOf(
                ACTION_ACCOUNT_ADDED,
                ACTION_ACCOUNT_REMOVED,
                ACTION_ACCOUNT_RENAMED
        ).forEach { action ->
            BroadcastsConnector.connect(
                    action,
                    connection
            )
        }
    }

    private fun initDefaultAccount(context: Context) {
        AccountManager.get(context)
                .takeIf { getAll(it).isEmpty() }
                ?.let {
                    add(it, "User") // TODO: Name translations
                }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Account.checkType(): Account {
        if (type != ACCOUNT_TYPE) throw IllegalArgumentException("Unsupported account type: $type")
        return this
    }

    private fun tryAdd(accountManager: AccountManager, account: Account,
                       accountId: String): Boolean =
            accountManager.addAccountExplicitly(
                    account,
                    null,
                    bundleOf(
                            KEY_ACCOUNT_ID to accountId
                    )
            )

    private fun tryRemove(accountManager: AccountManager, account: Account): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager.removeAccountExplicitly(account)
            } else {
                @Suppress("DEPRECATION")
                accountManager.removeAccount(account, null, null)
                true
            }

    fun add(context: Context, name: String): Account? =
            add(AccountManager.get(context), name)

    fun add(accountManager: AccountManager, name: String): Account? {
        val accountId = UUID.randomUUID().toString()
        val account = Account(name, ACCOUNT_TYPE)
        if (tryAdd(accountManager, account, accountId)) {
            LocalBroadcast.sendBroadcast(Intent(ACTION_ACCOUNT_ADDED)
                    .putExtra(EXTRA_ACCOUNT, account)
                    .putExtra(EXTRA_ACCOUNT_ID, accountId))

            return account
        }
        return null
    }

    fun remove(context: Context, account: Account): Boolean =
            remove(AccountManager.get(context), account)

    fun remove(accountManager: AccountManager, account: Account): Boolean {
        val accountId = getId(accountManager, account.checkType())
        if (tryRemove(accountManager, account)) {
            LocalBroadcast.sendBroadcast(Intent(ACTION_ACCOUNT_REMOVED)
                    .putExtra(EXTRA_ACCOUNT, account)
                    .putExtra(EXTRA_ACCOUNT_ID, accountId))

            return true
        }
        return false
    }

    fun get(context: Context, accountId: String): Account =
            get(AccountManager.get(context), accountId)

    fun get(accountManager: AccountManager, accountId: String): Account =
            getAll(accountManager).firstOrNull { getId(accountManager, it) == accountId }
                    ?: throw IllegalArgumentException("Account '$accountId' does not exist")

    fun getId(context: Context, account: Account): String =
            getId(AccountManager.get(context), account)

    fun getId(accountManager: AccountManager, account: Account): String =
            accountManager.getUserData(account.checkType(), KEY_ACCOUNT_ID)

    fun getAllWIthIds(context: Context): Map<String, Account> =
            getAllWIthIds(AccountManager.get(context))

    fun getAllWIthIds(accountManager: AccountManager): Map<String, Account> =
            getAll(accountManager).map { getId(accountManager, it) to it }.toMap()

    fun getAll(context: Context): Array<Account> =
            getAll(AccountManager.get(context))

    fun getAll(accountManager: AccountManager): Array<Account> =
            accountManager.getAccountsByType(ACCOUNT_TYPE)

    fun rename(context: Context, account: Account, newName: String): Account? =
            rename(AccountManager.get(context), account, newName)

    fun rename(accountManager: AccountManager, account: Account, newName: String): Account? {
        account.checkType()

        val accountId = getId(accountManager, account)
        val newAccount = Account(newName, ACCOUNT_TYPE)
        if (tryAdd(accountManager, newAccount, accountId)) {
            tryRemove(accountManager, account)

            LocalBroadcast.sendBroadcast(Intent(ACTION_ACCOUNT_RENAMED)
                    .putExtra(EXTRA_OLD_ACCOUNT, account)
                    .putExtra(EXTRA_ACCOUNT, newAccount)
                    .putExtra(EXTRA_ACCOUNT_ID, accountId))

            return newAccount
        }
        return null
    }
}
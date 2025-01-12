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
import android.accounts.AccountManagerFuture
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.R
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.account.save.ActiveAccount
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.runIf
import eu.codetopic.utils.UtilsBase
import org.jetbrains.anko.accountManager
import org.jetbrains.anko.bundleOf
import java.util.*

/**
 * @author anty
 */
object Accounts {

    private const val LOG_TAG = "Accounts"

    const val ACCOUNT_TYPE = "${BuildConfig.APPLICATION_ID}.account"

    const val ACTION_ACCOUNT_ADDED = "${BuildConfig.APPLICATION_ID}.account.ACTION_ACCOUNT_ADDED"
    const val ACTION_ACCOUNT_REMOVED = "${BuildConfig.APPLICATION_ID}.account.ACTION_ACCOUNT_REMOVED" // TODO: remove all account data when account is removed
    const val ACTION_ACCOUNT_RENAMED = "${BuildConfig.APPLICATION_ID}.account.ACTION_ACCOUNT_RENAMED"
    const val ACTION_ACCOUNTS_CHANGED = "${BuildConfig.APPLICATION_ID}.account.ACTION_ACCOUNTS_CHANGED"

    const val EXTRA_OLD_ACCOUNT = "cz.anty.purkynka.account.EXTRA_OLD_ACCOUNT"
    const val EXTRA_ACCOUNT = "cz.anty.purkynka.account.EXTRA_ACCOUNT"
    const val EXTRA_ACCOUNT_ID = "cz.anty.purkynka.account.EXTRA_ACCOUNT_ID"

    private const val KEY_ACCOUNT_ID = "cz.anty.purkynka.account.ACCOUNT_ID"

    var isInitialized = false
        private set

    fun initialize(context: Context, vararg notifyChannelIds: String) {
        if (UtilsBase.Process.isPrimaryProcess(context))
            initDefaultAccount(context)
        ActiveAccount.initialize(context)
        AccountNotifyGroup.init(context, *notifyChannelIds)
        isInitialized = true
    }

    private fun initDefaultAccount(context: Context) {
        context.accountManager
                .takeIf { getAll(it).isEmpty() }
                ?.let { add(context, it, context.getString(R.string.account_default_name)) }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Account.checkType(): Account {
        // FIXME: this way of checking account type can crash app, shouldn't we use more gentle way?
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
            add(context, context.accountManager, name)

    fun add(context: Context, accountManager: AccountManager, name: String): Account? {
        val accountId = UUID.randomUUID().toString()
        val account = Account(name, ACCOUNT_TYPE)
        if (tryAdd(accountManager, account, accountId)) {
            context.sendBroadcast(Intent(ACTION_ACCOUNT_ADDED)
                    .putExtra(EXTRA_ACCOUNT, account)
                    .putExtra(EXTRA_ACCOUNT_ID, accountId))
            context.sendBroadcast(Intent(ACTION_ACCOUNTS_CHANGED))

            return account
        }
        return null
    }

    fun requestAdd(activity: Activity, handler: Handler? = null,
                   callback: ((future: AccountManagerFuture<Bundle>) -> Unit)? = null) {
        activity.accountManager.addAccount(Accounts.ACCOUNT_TYPE, null,
                null, null, activity, callback, handler)
    }

    fun remove(context: Context, account: Account): Boolean =
            remove(context, context.accountManager, account)

    fun remove(context: Context, accountManager: AccountManager, account: Account): Boolean {
        val accountId = getId(accountManager, account.checkType())
        if (tryRemove(accountManager, account)) {
            context.sendBroadcast(Intent(ACTION_ACCOUNT_REMOVED)
                    .putExtra(EXTRA_ACCOUNT, account)
                    .putExtra(EXTRA_ACCOUNT_ID, accountId))
            context.sendBroadcast(Intent(ACTION_ACCOUNTS_CHANGED))

            return true
        }
        return false
    }

    fun get(context: Context, accountId: String): Account =
            get(context.accountManager, accountId)

    fun get(accountManager: AccountManager, accountId: String): Account =
            getOrNull(accountManager, accountId)
                    ?: throw IllegalArgumentException(
                            "Account with id '$accountId' does not exist"
                    )

    fun getOrNull(context: Context, accountId: String): Account? =
            getOrNull(context.accountManager, accountId)

    fun getOrNull(accountManager: AccountManager, accountId: String): Account? =
            getAll(accountManager).firstOrNull { getId(accountManager, it) == accountId }

    fun getByName(context: Context, accountName: String): Account =
            getByName(context.accountManager, accountName)

    @SuppressLint("NewApi")
    fun getByName(accountManager: AccountManager, accountName: String): Account =
            getByNameOrNull(accountManager, accountName)
                    ?: throw IllegalArgumentException(
                            "Account with name '$accountName' does not exist"
                    )

    fun getByNameOrNull(context: Context, accountName: String): Account? =
            getByNameOrNull(context.accountManager, accountName)

    @SuppressLint("NewApi")
    fun getByNameOrNull(accountManager: AccountManager, accountName: String): Account? =
            getAll(accountManager).let { accounts ->
                accounts.firstOrNull { it.name == accountName }
                        ?: runIf({ Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP }) {
                            accounts.firstOrNull {
                                accountManager.getPreviousName(it) == accountName
                            }
                        }
            }

    fun getId(context: Context, account: Account): String =
            getId(context.accountManager, account)

    fun getId(accountManager: AccountManager, account: Account): String =
            accountManager.getUserData(account.checkType(), KEY_ACCOUNT_ID) ?: run {
                Log.e(LOG_TAG, "getId()", RuntimeException("Id not found in user data"))

                val newAccountId = UUID.randomUUID().toString()
                accountManager.setUserData(account, KEY_ACCOUNT_ID, newAccountId)
                return@run newAccountId
            }

    fun getAllWIthIds(context: Context): Map<String, Account> =
            getAllWIthIds(context.accountManager)

    fun getAllWIthIds(accountManager: AccountManager): Map<String, Account> =
            getAll(accountManager).map { getId(accountManager, it) to it }.toMap()

    fun getAll(context: Context): Array<Account> =
            getAll(context.accountManager)

    fun getAll(accountManager: AccountManager): Array<Account> =
            accountManager.getAccountsByType(ACCOUNT_TYPE)

    fun rename(context: Context, account: Account, newName: String): Account? =
            rename(context, context.accountManager, account, newName)

    fun rename(context: Context, accountManager: AccountManager, account: Account, newName: String): Account? {
        account.checkType()

        if (account.name == newName) return account

        val accountId = getId(accountManager, account)
        val newAccount = Account(newName, ACCOUNT_TYPE)
        if (tryAdd(accountManager, newAccount, accountId)) {
            tryRemove(accountManager, account)

            context.sendBroadcast(Intent(ACTION_ACCOUNT_RENAMED)
                    .putExtra(EXTRA_OLD_ACCOUNT, account)
                    .putExtra(EXTRA_ACCOUNT, newAccount)
                    .putExtra(EXTRA_ACCOUNT_ID, accountId))
            context.sendBroadcast(Intent(ACTION_ACCOUNTS_CHANGED))

            return newAccount
        }
        return null
    }
}
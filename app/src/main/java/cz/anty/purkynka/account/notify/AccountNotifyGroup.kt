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

package cz.anty.purkynka.account.notify

import android.accounts.Account
import android.app.NotificationChannelGroup
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import android.support.annotation.MainThread
import android.support.annotation.RequiresApi
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.account.Accounts
import eu.codetopic.utils.receiver
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.notifications.manager.util.NotifyGroup

/**
 * @author anty
 */
class AccountNotifyGroup(val accountId: String, val account: Account, vararg channelIds: String) :
        NotifyGroup(idFor(accountId), *channelIds) {

    companion object {

        private const val LOG_TAG = "AccountNotifyGroup"

        private var channelIds: Array<out String>? = null

        fun idFor(accountId: String) =
                "${BuildConfig.APPLICATION_ID}.account.notify.group.$accountId"

        private val accountAddedReceiver: BroadcastReceiver =
                receiver { context, intent ->
                    intent ?: return@receiver
                    val account = intent.getParcelableExtra<Account>(Accounts.EXTRA_ACCOUNT) ?: return@receiver
                    val accountId = intent.getStringExtra(Accounts.EXTRA_ACCOUNT_ID) ?: return@receiver
                    val channelIds = channelIds ?: return@receiver

                    if (NotifyManager.hasGroup(idFor(accountId))) return@receiver
                    NotifyManager.installGroup(context, AccountNotifyGroup(accountId, account, *channelIds))
                }

        private val accountRemovedReceiver: BroadcastReceiver =
                receiver { context, intent ->
                    intent ?: return@receiver
                    val accountId = intent.getStringExtra(Accounts.EXTRA_ACCOUNT_ID) ?: return@receiver

                    if (!NotifyManager.hasGroup(idFor(accountId))) return@receiver
                    NotifyManager.uninstallGroup(context, idFor(accountId))
                }

        private val accountRenamedReceiver: BroadcastReceiver =
                receiver { context, intent ->
                    intent ?: return@receiver
                    val account = intent.getParcelableExtra<Account>(Accounts.EXTRA_ACCOUNT) ?: return@receiver
                    val accountId = intent.getStringExtra(Accounts.EXTRA_ACCOUNT_ID) ?: return@receiver
                    val channelIds = channelIds ?: return@receiver

                    if (!NotifyManager.hasGroup(idFor(accountId))) return@receiver
                    NotifyManager.replaceGroup(context, AccountNotifyGroup(accountId, account, *channelIds))
                }

        @MainThread
        internal fun init(context: Context, vararg channelIds: String) {
            if (this.channelIds != null) throw IllegalStateException("$LOG_TAG is still initialized")
            this.channelIds = channelIds

            val appContext = context.applicationContext
            appContext.registerReceiver(accountAddedReceiver,
                    intentFilter(Accounts.ACTION_ACCOUNT_ADDED))
            appContext.registerReceiver(accountRemovedReceiver,
                    intentFilter(Accounts.ACTION_ACCOUNT_REMOVED))
            appContext.registerReceiver(accountRenamedReceiver,
                    intentFilter(Accounts.ACTION_ACCOUNT_RENAMED))
            refresh(context)
        }

        @MainThread
        fun refresh(context: Context) {
            val channelIds = channelIds ?: throw IllegalStateException("$LOG_TAG is not initialized")
            Accounts.getAllWIthIds(context)
                    .filterNot { NotifyManager.hasGroup(idFor(it.key)) }
                    .forEach {
                        NotifyManager.installGroup(context,
                                AccountNotifyGroup(it.key, it.value, *channelIds))
                    }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createGroup(context: Context): NotificationChannelGroup =
            NotificationChannelGroup(id, account.name)
}
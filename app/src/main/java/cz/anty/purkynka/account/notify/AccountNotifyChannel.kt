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
import android.accounts.AccountManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.R
import cz.anty.purkynka.account.Accounts
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.notifications.manager.util.NotificationChannel
import eu.codetopic.utils.AndroidExtensions.getFormattedText
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.notifications.manager.NotificationsManager

/**
 * @author anty
 */
class AccountNotifyChannel(val accountId: String, val account: Account) : NotificationChannel(idFor(accountId)) {

    companion object {

        fun idFor(accountId: String) = "CHANNEL_ID{$accountId}"

        private val accountAddedReceiver: BroadcastReceiver =
                broadcast { context, intent ->
                    intent ?: return@broadcast
                    val account = intent.getParcelableExtra<Account>(Accounts.EXTRA_ACCOUNT) ?: return@broadcast
                    val accountId = intent.getStringExtra(Accounts.EXTRA_ACCOUNT_ID) ?: return@broadcast

                    if (NotificationsManager.existsChannel(idFor(accountId))) return@broadcast
                    NotificationsManager.initChannel(context, AccountNotifyChannel(accountId, account))
                }

        private val accountRemovedReceiver: BroadcastReceiver =
                broadcast { context, intent ->
                    intent ?: return@broadcast
                    val accountId = intent.getStringExtra(Accounts.EXTRA_ACCOUNT_ID) ?: return@broadcast

                    if (!NotificationsManager.existsChannel(idFor(accountId))) return@broadcast
                    NotificationsManager.removeChannel(context, idFor(accountId))
                }

        internal fun init(context: Context) {
            LocalBroadcast.registerReceiver(accountAddedReceiver,
                    intentFilter(Accounts.ACTION_ACCOUNT_ADDED))
            LocalBroadcast.registerReceiver(accountRemovedReceiver,
                    intentFilter(Accounts.ACTION_ACCOUNT_REMOVED))
            refresh(context)
        }

        fun refresh(context: Context) {
            Accounts.getAllWIthIds(context)
                    .filterNot { NotificationsManager.existsChannel(idFor(it.key)) }
                    .forEach {
                        NotificationsManager.initChannel(context,
                                AccountNotifyChannel(it.key, it.value))
                    }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel(context: Context): android.app.NotificationChannel =
            android.app.NotificationChannel(
                    id,
                    context.getFormattedText(R.string.notify_channel_grades_add, accountId), // TODO: better name
                    NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                setBypassDnd(false)
                setShowBadge(true)
                this.lightColor = ContextCompat.getColor(context, R.color.colorPrimary)
            }
}
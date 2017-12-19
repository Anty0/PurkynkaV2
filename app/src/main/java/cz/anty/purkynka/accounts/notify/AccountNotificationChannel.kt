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

package cz.anty.purkynka.accounts.notify

import android.accounts.AccountManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.R
import cz.anty.purkynka.accounts.AccountsHelper
import eu.codetopic.utils.AndroidExtensions.getFormattedText
import eu.codetopic.utils.notifications.manager.NotificationsManager
import eu.codetopic.utils.notifications.manager.util.NotificationChannel

/**
 * @author anty
 */
class AccountNotificationChannel(val accountId: String) : NotificationChannel(idFor(accountId)) {

    companion object {

        fun idFor(accountId: String) = "CHANNEL_ID{$accountId}"

        fun refresh(context: Context) {
            val accMan = AccountManager.get(context.applicationContext)
            AccountsHelper.getAllAccounts(accMan).asSequence()
                    .map { AccountsHelper.getAccountId(accMan, it) }
                    .filterNot { NotificationsManager.existsChannel(idFor(it)) }
                    .forEach {
                        NotificationsManager.initChannel(context, AccountNotificationChannel(it))
                    }
            // TODO: add to NotificationsManager cleanup function to cancel notifications for which no channel or group exists
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
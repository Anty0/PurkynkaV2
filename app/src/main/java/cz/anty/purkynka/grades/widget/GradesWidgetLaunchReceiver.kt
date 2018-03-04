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

package cz.anty.purkynka.grades.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cz.anty.purkynka.MainActivity
import cz.anty.purkynka.account.save.ActiveAccount
import cz.anty.purkynka.grades.GradesFragment

/**
 * @author anty
 */
class GradesWidgetLaunchReceiver : BroadcastReceiver() {

    companion object {

        private const val LOG_TAG = "GradesWidgetLaunchReceiver"

        private const val EXTRA_ACCOUNT_ID =
                "cz.anty.purkynka.grades.receiver.$LOG_TAG.EXTRA_ACCOUNT_ID"

        fun getIntent(context: Context, accountId: String?): Intent =
                Intent(context, GradesWidgetLaunchReceiver::class.java)
                        .putExtra(EXTRA_ACCOUNT_ID, accountId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)

        accountId?.let { ActiveAccount.set(it) }

        context.startActivity(
                MainActivity.getStartIntent(context, GradesFragment::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
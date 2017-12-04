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

package cz.anty.purkynka.grades.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import eu.codetopic.java.utils.log.Log

/**
 * @author anty
 */
class GradesClearDifferencesReceiver : BroadcastReceiver() {

    companion object {
        private const val LOG_TAG = "GradesClearDifferencesReceiver"

        const val EXTRA_ACCOUNT_ID = "cz.anty.purkynka.grades.notify.$LOG_TAG.EXTRA_ACCOUNT_ID"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val accountId = intent?.getStringExtra(EXTRA_ACCOUNT_ID)

        if (accountId == null) {
            Log.w(LOG_TAG, "onReceive(accountId=$accountId)",
                    IllegalArgumentException("AccountId is invalid"))
            return
        }

        GradesDataDifferences.instance.clearDiffs(accountId)
    }
}
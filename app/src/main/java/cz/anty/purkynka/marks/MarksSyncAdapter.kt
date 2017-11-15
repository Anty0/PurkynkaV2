/*
 * ApplicationPurkynka
 * Copyright (C)  2017  anty
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cz.anty.purkynka.marks

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import cz.anty.purkynka.Constants
import eu.codetopic.java.utils.log.Log

/**
 * @author anty
 */
class MarksSyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, false, true) {

    companion object {

        private const val LOG_TAG = "MarksSyncAdapter"

        const val CONTENT_AUTHORITY = MarksProvider.AUTHORITY
        const val SYNC_FREQUENCY: Long = Constants.SYNC_FREQUENCY_MARKS
    }

    override fun onPerformSync(account: Account, extras: Bundle, authority: String,
                               provider: ContentProviderClient, syncResult: SyncResult) {
        Log.d(LOG_TAG, "onPerformSync")

        if (authority != CONTENT_AUTHORITY) return

        TODO("not implemented")
    }
}
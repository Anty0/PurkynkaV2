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

package cz.anty.purkynka.grades.save

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
class GradesSyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, false, true) {

    companion object {

        private const val LOG_TAG = "GradesSyncAdapter"

        const val CONTENT_AUTHORITY = GradesProvider.AUTHORITY
        const val SYNC_FREQUENCY: Long = Constants.SYNC_FREQUENCY_GRADES
    }

    override fun onPerformSync(account: Account, extras: Bundle, authority: String,
                               provider: ContentProviderClient, syncResult: SyncResult) {
        Log.d(LOG_TAG, "onPerformSync")

        if (authority != CONTENT_AUTHORITY) return

        TODO("not implemented")
    }
}
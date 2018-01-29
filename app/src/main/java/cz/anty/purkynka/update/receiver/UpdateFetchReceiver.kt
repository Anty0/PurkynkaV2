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

package cz.anty.purkynka.update.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cz.anty.purkynka.update.sync.UpdateCheckJob
import eu.codetopic.java.utils.log.Log
import org.jetbrains.anko.bundleOf

/**
 * @author anty
 */
class UpdateFetchReceiver : BroadcastReceiver() {

    companion object {

        private const val LOG_TAG = "UpdateFetchReceiver"

        fun getIntent(context: Context): Intent =
                Intent(context, UpdateFetchReceiver::class.java)
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val result = UpdateCheckJob.fetchUpdates()

            if (isOrderedBroadcast) {
                setResult(UpdateCheckJob.REQUEST_RESULT_OK, null, bundleOf(
                        UpdateCheckJob.REQUEST_EXTRA_RESULT to result
                ))
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "onReceive()", e)

            if (isOrderedBroadcast) {
                setResult(UpdateCheckJob.REQUEST_RESULT_FAIL, null, bundleOf(
                        UpdateCheckJob.REQUEST_EXTRA_THROWABLE to e
                ))
            }
        }
    }
}
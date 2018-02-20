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
import eu.codetopic.java.utils.log.Log

/**
 * @author anty
 */
class AppUpdatedReceiver : BroadcastReceiver() {

    companion object {

        private const val LOG_TAG = "AppUpdatedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Log.b(LOG_TAG, "onReceive() -> Received app replaced intent")

        // TODO: implement (save last known version code and if...)
    }
}
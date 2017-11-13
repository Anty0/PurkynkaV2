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

import android.app.IntentService
import android.app.Service
import android.content.Intent
import android.os.IBinder

import java.util.Calendar

import cz.anty.purkynka.Constants
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.timing.info.TimedComponent

/**
 * @author anty
 */
class MarksSyncService : Service() {

    companion object {

        private const val LOG_TAG = "MarksSyncService"

        private val sSyncAdapterLock = Any()
        private var sSyncAdapter: MarksSyncAdapter? = null
    }

    override fun onCreate() {
        super.onCreate()
        synchronized(sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = MarksSyncAdapter(applicationContext)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return sSyncAdapter?.syncAdapterBinder
    }
}

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

package cz.anty.purkynka.grades.save

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * @author anty
 */
class GradesSyncService : Service() {

    companion object {

        private const val LOG_TAG = "GradesSyncService"

        private val sSyncAdapterLock = Any()
        private var sSyncAdapter: GradesSyncAdapter? = null
    }

    override fun onCreate() {
        super.onCreate()
        synchronized(sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = GradesSyncAdapter(applicationContext)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return sSyncAdapter?.syncAdapterBinder
    }
}

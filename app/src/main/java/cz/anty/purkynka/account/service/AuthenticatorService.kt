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

package cz.anty.purkynka.account.service

import android.accounts.AccountManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * @author anty
 */
class AuthenticatorService : Service() {

    companion object {

        private const val LOG_TAG = "AuthenticatorService"
    }

    private var sAuthenticator: AuthenticatorImpl? = null

    override fun onCreate() {
        super.onCreate()
        sAuthenticator = AuthenticatorImpl(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return when (intent?.action) {
            AccountManager.ACTION_AUTHENTICATOR_INTENT -> sAuthenticator?.iBinder
            else -> null
        }
    }

    override fun onDestroy() {
        sAuthenticator = null
        super.onDestroy()
    }
}
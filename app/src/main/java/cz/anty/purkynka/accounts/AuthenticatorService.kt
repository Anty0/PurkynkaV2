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

package cz.anty.purkynka.accounts

import android.accounts.AccountManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Created by anty on 10/9/17.
 * @author anty
 */
class AuthenticatorService : Service() {

    private var mAuthenticator: AuthenticatorImpl? = null

    override fun onCreate() {
        super.onCreate()
        mAuthenticator = AuthenticatorImpl(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == AccountManager.ACTION_AUTHENTICATOR_INTENT) {
            return mAuthenticator?.iBinder
        }
        return null
    }

    override fun onDestroy() {
        mAuthenticator = null
        super.onDestroy()
    }
}
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

package cz.anty.purkynka.accounts

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
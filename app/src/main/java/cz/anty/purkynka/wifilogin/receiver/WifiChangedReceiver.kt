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

package cz.anty.purkynka.wifilogin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.wifilogin.load.WifiLoginFetcher
import cz.anty.purkynka.wifilogin.save.WifiData
import cz.anty.purkynka.wifilogin.save.WifiLoginData
import eu.codetopic.java.utils.JavaExtensions.alsoIf
import eu.codetopic.java.utils.log.Log
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * @author anty
 */
class WifiChangedReceiver : BroadcastReceiver() {

    companion object {

        private const val LOG_TAG = "WifiChangedReceiver"

        private const val DELAY_AUTO_LOGIN = 1_000L // 1 second in milis
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WifiManager.WIFI_STATE_CHANGED_ACTION
                && intent.action != ConnectivityManager.CONNECTIVITY_ACTION) return

        val pResult = goAsync()
        launch(UI) {
            try {
                val accountId = bg {
                    Accounts.getAll(context).firstOrNull()
                            ?.let { Accounts.getId(context, it) }
                }.await() ?: return@launch

                val (username, password) = bg {
                    WifiLoginData.loginData
                            .takeIf { it.isLoggedIn(accountId) }
                            ?.getCredentials(accountId)
                }.await() ?: return@launch

                if (username == null || password == null) return@launch

                delay(DELAY_AUTO_LOGIN)

                WifiLoginFetcher.tryLoginBackground(context, username, password).await()
                        .alsoIf({ it == WifiLoginFetcher.LoginResult.SUCCESS }) {
                            WifiData.instance.incrementLoginCounter(accountId)
                        }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "onReceive()", e)
            } finally {
                pResult.finish()
            }
        }
    }
}
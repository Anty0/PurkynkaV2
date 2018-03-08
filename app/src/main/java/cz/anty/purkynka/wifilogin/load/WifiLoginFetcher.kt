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

package cz.anty.purkynka.wifilogin.load

import android.content.Context
import android.support.annotation.MainThread
import android.support.annotation.UiThread
import cz.anty.purkynka.R
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.substringOrNull
import eu.codetopic.utils.getFormattedText
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.alert
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.coroutines.experimental.Ref
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.toast
import org.jetbrains.anko.wifiManager
import org.jsoup.Connection
import org.jsoup.Jsoup
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * @author anty
 */
object WifiLoginFetcher {

    private const val LOG_TAG = "WifiLoginFetcher"

    private const val WIFI_NAME = "ISSWF"

    private const val TEST_URL = "http://www.sspbrno.cz/"
    private const val LOGIN_URL_BASE = "wifi.sspbrno.cz"
    //private const val LOGIN_URL = "http://wifi.sspbrno.cz/login.html";
    //private const val LOGOUT_URL = "http://wifi.sspbrno.cz/logout.html";
    private const val LOGIN_FIELD = "username"
    private const val PASS_FIELD = "password"
    //private const val SUBMIT = "Submit";
    //private const val SUBMIT_VALUE = "Submit";

    @MainThread
    fun tryLoginBackground(context: Context, username: String, password: String,
                           force: Boolean = false): Deferred<LoginResult> {
        val (wifiCorrect, wifiName) = testWifi(context)

        if (!force && !wifiCorrect) return CompletableDeferred(LoginResult.FAIL_INVALID_WIFI)

        val contextRef = context.applicationContext.asReference()

        return async login@ {
            try {
                return@login (checkForLoginUrl() ?: return@login LoginResult.FAIL_NO_LOGIN_PAGE)
                        .also {
                            contextRef.toastCh {
                                if (wifiName == null)
                                    it.getText(R.string.toast_wifi_logging_start_unknown)
                                else it.getFormattedText(R.string.toast_wifi_logging_start, wifiName)
                            }
                        }
                        .also { requestLogin(it, username, password) }
                        .also { contextRef.toastR { R.string.toast_wifi_logging_success } }
                        .let { LoginResult.SUCCESS }
            } catch (e: Exception) {
                Log.w(LOG_TAG, "tryLoginBackground(force=$force)", e)
                contextRef.toastR { R.string.toast_wifi_logging_fail }
                return@login LoginResult.FAIL_CONNECTION
            }
        }
    }

    @UiThread
    fun tryLoginGui(context: Context, username: String, password: String,
                    force: ForceLogin = ForceLogin.ASK): Deferred<LoginResult> {
        val (wifiCorrect, _) = testWifi(context)

        val contextRef = context.asReference()

        if (force != ForceLogin.YES && !wifiCorrect) {
            return if (force == ForceLogin.ASK)
                async(UI) { contextRef.alertWifiCheckFail(username, password) }
            else CompletableDeferred(LoginResult.FAIL_INVALID_WIFI)
        }

        return async login@ {
            try {
                return@login (checkForLoginUrl() ?: return@login LoginResult.FAIL_NO_LOGIN_PAGE)
                        .also { requestLogin(it, username, password) }
                        .let { LoginResult.SUCCESS }
            } catch (e: Exception) {
                Log.w(LOG_TAG, "tryLoginGui(force=$force)", e)
                return@login LoginResult.FAIL_CONNECTION
            }
        }
    }

    private suspend fun Ref<Context>.alertWifiCheckFail(username: String, password: String): LoginResult {
        val context = this()
        return suspendCoroutine { continuation ->
            context.alert(Appcompat, R.string.dialog_text_wifi_unknown,
                    R.string.dialog_title_wifi_unknown) {
                positiveButton(R.string.but_continue) {
                    it.dismiss()
                    launch(UI) {
                        continuation.resume(tryLoginGui(context,
                                username, password, ForceLogin.YES).await())
                    }
                }
                negativeButton(R.string.but_cancel) {
                    it.dismiss()
                    continuation.resume(LoginResult.FAIL_INVALID_WIFI)
                }
                onCancelled {
                    continuation.resume(LoginResult.FAIL_INVALID_WIFI)
                }
            }.show()
        }
    }

    private inline fun Ref<Context>.toastCh(crossinline block: (context: Context) -> CharSequence) =
            launch(UI) { this@toastCh().also { it.toast(block(it)) } }

    private inline fun Ref<Context>.toastR(crossinline block: (context: Context) -> Int) =
            toastCh { it.getText(block(it)) }

    private fun testWifi(context: Context): Pair<Boolean, String?> =
            context.applicationContext.wifiManager.connectionInfo?.ssid
                    .let { (it?.contains(WIFI_NAME) ?: false) to it }

    private fun checkForLoginUrl(): String? = try {
        @Suppress("DEPRECATION")
        Jsoup.connect(TEST_URL)
                .followRedirects(true)
                .validateTLSCertificates(false) // FIXME: use ThrustManager to allow only one certificate
                .execute().body()
                //.also { Log.d(LOG_TAG, "checkForLoginUrl() -> (testPageBody=$it)") }
                .substringOrNull("<META http-equiv=\"refresh\" content=\"1; URL=", "\">")
                .also { Log.d(LOG_TAG, "checkForLoginUrl() -> (loginUrl=$it)") }
                ?.takeIf { it.contains(LOGIN_URL_BASE) }
    } catch (e: Exception) {
        Log.w(LOG_TAG, "checkForLoginUrl()", e); null
    }

    @Suppress("DEPRECATION")
    private fun requestLogin(loginUrl: String, username: String, password: String) =
            Jsoup.connect(loginUrl)
                    .data(
                            "buttonClicked", "4", // -\
                            "err_flag", "0", // -------\
                            "err_msg", "", // ----------> Just some stuff required by login page.
                            "info_flag", "0", // ------/
                            "info_msg", "", // -------/
                            "redirect_url", "", // --/
                            LOGIN_FIELD, username,
                            PASS_FIELD, password
                    )
                    .method(Connection.Method.POST)
                    .validateTLSCertificates(false) // FIXME: use ThrustManager to allow only one certificate
                    .execute()

    enum class LoginResult {
        SUCCESS, FAIL_INVALID_WIFI, FAIL_NO_LOGIN_PAGE, FAIL_CONNECTION
    }

    enum class ForceLogin {
        YES, ASK, NO
    }
}
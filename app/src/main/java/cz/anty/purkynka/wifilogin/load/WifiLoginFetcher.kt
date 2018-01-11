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

/**
 * @author anty
 */
object WifiLoginFetcher {

    fun tryLogin(username: String, password:String) {
        // TODO: implement
    }

    /*val WIFI_NAME = "ISSWF"
    private val LOG_TAG = "WifiLogin"
    private val TEST_URL = "http://www.sspbrno.cz/"
    //private static final String LOGIN_URL = "http://wifi.sspbrno.cz/login.html";
    //private static final String LOGOUT_URL = "http://wifi.sspbrno.cz/logout.html";
    private val LOGIN_FIELD = "username"
    private val PASS_FIELD = "password"
    //private static final String SUBMIT = "Submit";
    //private static final String SUBMIT_VALUE = "Submit";

    private fun showToast(context: Context, threadHandler: Handler,
                          text: CharSequence, showToasts: Boolean) {
        if (showToasts)
            threadHandler.post(Runnable { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() })
    }

    @Throws(IOException::class)
    fun tryLogin(context: Context, username: String, password: String,
                 threadHandler: Handler, wifiName: String,
                 showToasts: Boolean): Boolean {
        var showFailNotification = false
        try {
            val loginUrl = getLoginUrl()
            if (loginUrl == null || !loginUrl
                    .contains("wifi.sspbrno.cz/login.html"))
                return false

            showToast(context, threadHandler, Utils.getFormattedText(context, R.string
                    .toast_text_logging_to_wifi, wifiName), showToasts)
            showFailNotification = true
            sendLoginRequest(loginUrl, username, password)
            showToast(context, threadHandler, Utils.getFormattedText(context, R.string
                    .toast_text_logged_in_wifi, wifiName), showToasts)
            return true
        } catch (t: Throwable) {
            showToast(context, threadHandler, Utils.getFormattedText(context, R.string
                    .toast_text_failed_logging_to_wifi, wifiName),
                    showToasts && showFailNotification)
            throw t
        }

    }

    @Throws(IOException::class)
    private fun getLoginUrl(): String? {
        var exception: IOException? = null
        for (i in 0 until Constants.MAX_TRY) {
            try {
                val body = Jsoup.connect(TEST_URL)
                        .validateTLSCertificates(false)
                        .execute().body()
                Log.d(LOG_TAG, "getLoginUrl body: " + body)
                val toFind = "<META http-equiv=\"refresh\" content=\"1; URL="
                var url: String
                var index = body.indexOf(toFind)
                if (index != -1) {
                    index += toFind.length
                    url = body.substring(index, body.indexOf("\">", index))
                    Log.d(LOG_TAG, "getLoginUrl loginUrlBefore: " + url)
                    index = url.indexOf("?")
                    if (index != -1)
                        url = url.substring(0, index)
                    Log.d(LOG_TAG, "getLoginUrl loginUrl: " + url)
                    return url
                }
                return null
            } catch (e: IOException) {
                if (e.getCause() == null && exception != null)
                    e.initCause(exception)
                exception = e
                Log.d(LOG_TAG, "getLoginUrl", exception)
            }

        }
        throw exception
    }

    @Throws(IOException::class)
    private fun sendLoginRequest(url: String, username: String, password: String) {
        var exception: IOException? = null
        for (i in 0 until Constants.MAX_TRY) {
            try {
                Jsoup.connect(url)
                        .data("buttonClicked", "4", "err_flag", "0", "err_msg", "", "info_flag", "0", "info_msg", "",
                                "redirect_url", "", LOGIN_FIELD, username, PASS_FIELD, password)
                        .method(Connection.Method.POST)
                        .validateTLSCertificates(false)
                        .execute()
                return
            } catch (e: IOException) {
                if (e.getCause() == null && exception != null)
                    e.initCause(exception)
                exception = e
                Log.d(LOG_TAG, "sendLoginRequest", exception)
            }

        }
        throw exception
    }*/
}
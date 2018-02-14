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

package cz.anty.purkynka.lunches.load

import android.support.annotation.WorkerThread
import cz.anty.purkynka.Utils
import cz.anty.purkynka.exceptions.LoginExpiredException
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.JavaExtensions.alsoIfNot
import eu.codetopic.java.utils.JavaExtensions.ifTrue
import org.jsoup.Jsoup
import kotlinx.io.IOException
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.SimpleDateFormat
import java.util.*


/**
 * @author anty
 */
object LunchesFetcher {

    private const val LOG_TAG = "LunchesFetcher"

    private const val PARAM_USERNAME = "j_username"
    private const val PARAM_PASSWORD = "j_password"
    private const val PARAM_REMEMBER_ME = "_spring_security_remember_me"
    private const val PARAM_REMEMBER_ME_VAL = "false"
    private const val PARAM_TERMINAL = "terminal"
    private const val PARAM_TERMINAL_VAL = "false"
    private const val PARAM_TYPE = "type"
    private const val PARAM_TYPE_VAL = "web"
    private const val PARAM_TARGET_URL = "targetUrl"
    private const val PARAM_TARGET_URL_VAL = "/faces/secured/main.jsp?terminal=false&amp;status=true&amp;printer=false&amp;keyboard=false"
    private const val PARAM_PRINTER = "printer"
    private const val PARAM_PRINTER_VAL = "false"
    private const val PARAM_KEYBOARD = "terminal"
    private const val PARAM_KEYBOARD_VAL = "false"
    private const val PARAM_DAY = "day"
    private val PARAM_DAY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    private const val URL_LOGIN = "http://stravovani.sspbrno.cz:8080/faces/j_spring_security_check"
    // username password remember_me terminal type target_url

    private const val URL_LOGOUT = "http://stravovani.sspbrno.cz:8080/j_spring_security_logout"
    // terminal keyboard printer // ?terminal=false&keyboard=false&printer=false

    private const val URL_BURZA = "http://stravovani.sspbrno.cz:8080/faces/secured/burza.jsp"
    // terminal keyboard printer // ?terminal=false&keyboard=false&printer=false

    private const val URL_MAIN = "http://stravovani.sspbrno.cz:8080/faces/secured/main.jsp"
    // terminal keyboard printer // ?terminal=false&keyboard=false&printer=false

    private const val URL_MONTH = "http://stravovani.sspbrno.cz:8080/faces/secured/month.jsp"
    // terminal keyboard printer // ?terminal=false&keyboard=false&printer=false

    private const val URL_DAY = "http://stravovani.sspbrno.cz:8080/faces/secured/main.jsp"
    // day terminal printer keyboard // ?day=2018-02-23&terminal=false&printer=false&keyboard=false

    private const val URL_START_ORDER = "http://stravovani.sspbrno.cz:8080/faces/secured/"
    // + url_add

    @WorkerThread
    @Throws(IOException::class)
    fun login(username: String, password: String): Map<String, String> =
            Jsoup.connect(URL_LOGIN)
                    .userAgent(Utils.userAgent)
                    .data(
                            PARAM_USERNAME, username,
                            PARAM_PASSWORD, password,
                            PARAM_REMEMBER_ME, PARAM_REMEMBER_ME_VAL,
                            PARAM_TERMINAL, PARAM_TERMINAL_VAL,
                            PARAM_TYPE, PARAM_TYPE_VAL,
                            PARAM_TARGET_URL, PARAM_TARGET_URL_VAL
                    )
                    .followRedirects(false)
                    .method(Connection.Method.POST)
                    .execute().cookies()

    @WorkerThread
    @Throws(IOException::class)
    fun logout(loginCookies: Map<String, String>): Document =
            Jsoup.connect(URL_LOGOUT)
                    .userAgent(Utils.userAgent)
                    .data(
                            PARAM_TERMINAL, PARAM_TERMINAL_VAL,
                            PARAM_KEYBOARD, PARAM_KEYBOARD_VAL,
                            PARAM_PRINTER, PARAM_PRINTER_VAL
                    )
                    .followRedirects(false)
                    .method(Connection.Method.GET)
                    .cookies(loginCookies)
                    .get()

    @WorkerThread
    @Throws(IOException::class)
    fun isLoggedIn(loginCookies: Map<String, String>): Boolean =
            isLoggedIn(getPage(URL_MAIN, loginCookies))

    fun isLoggedIn(page: Document): Boolean =
            page.select("div.login_menu").isEmpty() &&
                    !page.select("div.topMenu").isEmpty() &&
                    "stravovani.sspbrno.cz" in page.location()

    @WorkerThread
    @Throws(IOException::class)
    fun orderLunch(loginCookies: Map<String, String>, orderUrl: String) {
        val response = Jsoup
                .connect(URL_START_ORDER + orderUrl.replace("&amp;", "&"))
                .cookies(loginCookies)
                .execute()

        Log.v(LOG_TAG, "orderLunch() -> (response=${response.body()})")

        try {
            response.body()
                    .let { JSONObject(it) }
                    .takeIf { it.has("error") }
                    ?.getBoolean("error")
        } catch (e: Exception) {
            Log.d(LOG_TAG, "orderLunch() -> " +
                    "Non json response received, testing response using fallback strategy")

            "\"error\":true" in response.body()
        } ifTrue {
            throw IOException("Server error received")
        }
    }

    @WorkerThread
    @Throws(IOException::class)
    fun getLunchOptionsGroupElement(loginCookies: Map<String, String>, date: Long): Element =
            Jsoup.connect(URL_DAY)
                    .userAgent(Utils.userAgent)
                    .data(
                            PARAM_DAY, PARAM_DAY_FORMAT.format(date),
                            PARAM_TERMINAL, PARAM_TERMINAL_VAL,
                            PARAM_PRINTER, PARAM_PRINTER_VAL,
                            PARAM_KEYBOARD, PARAM_KEYBOARD_VAL
                    )
                    .followRedirects(false)
                    .method(Connection.Method.GET)
                    .cookies(loginCookies)
                    .get()
                    .select("div#mainContext")
                    .select("table")
                    .select("td")
                    .get(0)

    @WorkerThread
    @Throws(IOException::class)
    fun getLunchOptionsGroupsElements(loginCookies: Map<String, String>): Elements =
            Jsoup.connect(URL_MONTH)
                    .userAgent(Utils.userAgent)
                    .data(
                            PARAM_TERMINAL, PARAM_TERMINAL_VAL,
                            PARAM_KEYBOARD, PARAM_KEYBOARD_VAL,
                            PARAM_PRINTER, PARAM_PRINTER_VAL
                    )
                    .followRedirects(false)
                    .method(Connection.Method.GET)
                    .cookies(loginCookies)
                    .get()
                    .select("div#mainContext")
                    .select("table")
                    .select("form[name=objednatJidlo-]")


    @WorkerThread
    @Throws(IOException::class)
    fun getLunchesBurzaElements(loginCookies: Map<String, String>): Elements =
            Jsoup.connect(URL_BURZA)
                    .userAgent(Utils.userAgent)
                    .data(
                            PARAM_TERMINAL, PARAM_TERMINAL_VAL,
                            PARAM_KEYBOARD, PARAM_KEYBOARD_VAL,
                            PARAM_PRINTER, PARAM_PRINTER_VAL
                    )
                    .followRedirects(false)
                    .method(Connection.Method.GET)
                    .cookies(loginCookies)
                    .get()
                    .select("div#mainContext")
                    .select("table")
                    .select("tr")
                    .also { it.removeAt(0) } // remove first line of table, which is not lunch

    @WorkerThread
    @Throws(IOException::class)
    fun getMainPage(loginCookies: Map<String, String>): Document =
            Jsoup.connect(URL_MAIN)
                    .userAgent(Utils.userAgent)
                    .data(
                            PARAM_TERMINAL, PARAM_TERMINAL_VAL,
                            PARAM_KEYBOARD, PARAM_KEYBOARD_VAL,
                            PARAM_PRINTER, PARAM_PRINTER_VAL
                    )
                    .followRedirects(false)
                    .method(Connection.Method.GET)
                    .cookies(loginCookies)
                    .get()

    @WorkerThread
    @Throws(IOException::class)
    @Deprecated("")
    private fun getPage(url: String, loginCookies: Map<String, String>): Document =
            Jsoup.connect(url)
                    .userAgent(Utils.userAgent)
                    .followRedirects(false)
                    .cookies(loginCookies)
                    .get()
}
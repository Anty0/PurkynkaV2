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


/**
 * @author anty
 */
object LunchesFetcher {

    private const val LOG_TAG = "LunchesFetcher"

    private const val URL_LOGIN = "http://stravovani.sspbrno.cz:8080/faces/j_spring_security_check"
    private const val PARAM_USERNAME = "j_username"
    private const val PARAM_PASSWORD = "j_password"
    private const val PARAM_REMEMBER_ME = "_spring_security_remember_me"
    private const val PARAM_REMEMBER_ME_VAL = "true"
    private const val PARAM_TERMINAL = "terminal"
    private const val PARAM_TERMINAL_VAL = "false"
    private const val PARAM_TYPE = "type"
    private const val PARAM_TYPE_VAL = "web"
    private const val PARAM_TARGET_URL = "targetUrl"
    private const val PARAM_TARGET_URL_VAL = "/faces/secured/main.jsp?terminal=false&amp;status=true&amp;printer=false&amp;keyboard=false"

    private const val URL_BURZA = "http://stravovani.sspbrno.cz:8080/faces/secured/burza.jsp?terminal=false&keyboard=false&printer=false"
    private const val URL_MAIN = "http://stravovani.sspbrno.cz:8080/faces/secured/main.jsp?terminal=false&keyboard=false&printer=false"
    private const val URL_MONTH = "http://stravovani.sspbrno.cz:8080/faces/secured/month.jsp?terminal=false&keyboard=false&printer=false"

    private const val URL_START_ORDER = "http://stravovani.sspbrno.cz:8080/faces/secured/"

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

    @Throws(IOException::class)
    fun isLoggedIn(loginCookies: Map<String, String>): Boolean =
            isLoggedIn(getPage(URL_MAIN, loginCookies))

    fun isLoggedIn(page: Document): Boolean =
            page.select("div.login_menu").isEmpty() &&
                    !page.select("div.topMenu").isEmpty() &&
                    "stravovani.sspbrno.cz" in page.location()

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
            Log.w(LOG_TAG, "orderLunch() -> " +
                    "Invalid response received, testing response using fallback strategy")

            "\"error\":true" in response.body()
        } ifTrue {
            throw IOException("Server error received")
        }
    }

    @Throws(IOException::class)
    fun getLunchOptionsGroupsElements(loginCookies: Map<String, String>): Elements =
            getPage(URL_MONTH, loginCookies)
                    .alsoIfNot({ isLoggedIn(it) }) { throw LoginExpiredException() }
                    .select("div#mainContext")
                    .select("table")
                    .select("form[name=objednatJidlo-]")
                    .also { Log.v(LOG_TAG, "getLunchOptionsGroupsElements() -> (result=$it)") }


    @Throws(IOException::class)
    fun getBurzaLunchesElements(loginCookies: Map<String, String>): Elements =
            getPage(URL_BURZA, loginCookies)
                    .alsoIfNot({ isLoggedIn(it) }) { throw LoginExpiredException() }
                    .select("div#mainContext")
                    .select("table")
                    .select("tr")
                    .also { it.removeAt(0) } // remove first line of table, which is not lunch
                    .also { Log.v(LOG_TAG, "getBurzaLunchesElements() -> (result=$it)") }

    fun getCredit(elements: Element): Float =
            elements.select("span#Kredit span").text().toFloat()

    @Throws(IOException::class)
    fun getMainPage(loginCookies: Map<String, String>): Document =
            getPage(URL_MAIN, loginCookies)
                    .alsoIfNot({ isLoggedIn(it) }) { throw LoginExpiredException() }

    @Throws(IOException::class)
    private fun getPage(url: String, loginCookies: Map<String, String>): Document =
            Jsoup.connect(url)
                    .userAgent(Utils.userAgent)
                    .followRedirects(false)
                    .cookies(loginCookies)
                    .get()
}
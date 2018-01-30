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
import eu.codetopic.java.utils.log.Log
import org.jsoup.Jsoup
import kotlinx.io.IOException
import org.jsoup.Connection
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements


/**
 * @author anty
 */
object LunchesFetcher {

    private const val LOG_TAG = "LunchesFetcher"

    private const val LOGIN_URL = "http://stravovani.sspbrno.cz:8080/faces/j_spring_security_check"
    private const val LOGIN_FIELD = "j_username"
    private const val PASS_FIELD = "j_password"
    private const val CHECKBOX_SAVE = "_spring_security_remember_me"
    private const val CHECKBOX_SAVE_VALUE = "true"
    private const val TERMINAL = "terminal"
    private const val TERMINAL_VALUE = "false"
    private const val TYPE = "type"
    private const val TYPE_VALUE = "web"
    private const val TARGET_URL = "targetUrl"
    private const val TARGET_URL_VALUE = "/faces/secured/main.jsp?terminal=false&amp;status=true&amp;printer=false&amp;keyboard=false"

    private const val BURZA_URL = "http://stravovani.sspbrno.cz:8080/faces/secured/burza.jsp?terminal=false&keyboard=false&printer=false"
    private const val MAIN_URL = "http://stravovani.sspbrno.cz:8080/faces/secured/main.jsp?terminal=false&keyboard=false&printer=false"
    private const val MONTH_URL = "http://stravovani.sspbrno.cz:8080/faces/secured/month.jsp?terminal=false&keyboard=false&printer=false"

    private const val ORDER_URL_START = "http://stravovani.sspbrno.cz:8080/faces/secured/"

    @Throws(IOException::class)
    fun login(username: String, password: String): Map<String, String> =
            Jsoup.connect(LOGIN_URL)
                    .userAgent(Utils.userAgent)
                    .data(LOGIN_FIELD, username, PASS_FIELD, password, CHECKBOX_SAVE, CHECKBOX_SAVE_VALUE,
                            TERMINAL, TERMINAL_VALUE, TYPE, TYPE_VALUE, TARGET_URL, TARGET_URL_VALUE)
                    .followRedirects(false)
                    .method(Connection.Method.POST)
                    .execute().cookies()

    @Throws(IOException::class)
    fun isLoggedIn(loginCookies: Map<String, String>): Boolean = isLoggedIn(getPage(MAIN_URL, loginCookies))

    fun isLoggedIn(page: Document): Boolean =
            page.select("div.login_menu").isEmpty() &&
                    !page.select("div.topMenu").isEmpty() &&
                    "stravovani.sspbrno.cz" in page.location()

    @Throws(IOException::class)
    fun orderLunch(loginCookies: Map<String, String>, urlAdd: String) {
        val response = Jsoup
                .connect(ORDER_URL_START + urlAdd.replace("&amp;", "&"))
                .cookies(loginCookies)
                .execute()

        Log.v(LOG_TAG, "orderLunch() -> (response=${response.body()})")
        if ("\"error\":true" in response.body())
            throw IOException("Server error received")
    }

    @Throws(IOException::class)
    fun getMonthElements(loginCookies: Map<String, String>): Elements {
        val monthPage = getPage(MONTH_URL, loginCookies)
        if (!isLoggedIn(monthPage))
            throw IllegalStateException("$LOG_TAG is not logged in")

        val result = monthPage
                .select("div#mainContext")
                .select("table")
                .select("form[name=objednatJidlo-]")
        Log.v(LOG_TAG, "getMonthElements() -> (result=$result)")
        return result
    }


    @Throws(IOException::class)
    fun getBurzaElements(loginCookies: Map<String, String>): Elements {
        val burzaPage = getPage(BURZA_URL, loginCookies)
        if (!isLoggedIn(burzaPage))
            throw IllegalStateException("$LOG_TAG is not logged in")

        val result = burzaPage
                .select("div#mainContext")
                .select("table")
                .select("tr")
        result.removeAt(0)
        Log.v(LOG_TAG, "getBurzaElements() -> (result=$result)")
        return result
    }

    fun getCredit(elements: Element): Double =
            elements.select("span#Kredit span").text().toDouble()

    @Throws(IOException::class)
    fun getMainPage(loginCookies: Map<String, String>): Document =
            getPage(MAIN_URL, loginCookies)

    @Throws(IOException::class)
    private fun getPage(url: String, loginCookies: Map<String, String>): Document =
            Jsoup.connect(url)
                    .userAgent(Utils.userAgent)
                    .followRedirects(false)
                    .cookies(loginCookies)
                    .get()
}
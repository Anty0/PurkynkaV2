/*
 * app
 * Copyright (C)   2017  anty
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

package cz.anty.purkynka.grades.load

import android.support.annotation.WorkerThread

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import java.io.IOException

import cz.anty.purkynka.Constants
import cz.anty.purkynka.Utils
import cz.anty.purkynka.exceptions.LoginExpiredException
import cz.anty.purkynka.grades.data.Semester

import eu.codetopic.java.utils.JavaExtensions.alsoIfNot

/**
 * @author anty
 */
object GradesFetcher {

    private const val LOG_TAG = "GradesFetcher"

    //private const val URL_SCHOOL = "http://www.sspbrno.cz";
    private const val URL_MAIN = "https://isas.sspbrno.cz" // TODO: use Firebase shared config

    private const val URL_ADD_LOGIN = "/prihlasit.php"
    private const val PARAM_USERNAME = "login-isas-username"
    private const val PARAM_PASSWORD = "login-isas-password"
    private const val PARAM_SUBMIT = "login-isas-send"
    private const val PARAM_SUBMIT_VAL = "isas-send"

    private const val URL_ADD_LOGOUT = "/odhlasit.php"

    private const val URL_ADD_GRADES = "/prubezna-klasifikace.php"
    private const val PARAM_SEMESTER = "pololeti"
    private const val PARAM_SHOW = "zobraz"
    private const val PARAM_SHOW_VAL_BY_DATE = "datum"
    //private const val PARAM_SHOW_VAL_BY_LESSONS = "predmety";
    //private const val PARAM_SHOW_VAL_BY_SCORE = "hodnoceni";

    @WorkerThread
    @Throws(IOException::class)
    fun login(username: String, password: String): Map<String, String> {
        return Jsoup
                .connect(URL_MAIN + URL_ADD_LOGIN)
                .userAgent(Utils.userAgent)
                .data(
                        PARAM_USERNAME, username,
                        PARAM_PASSWORD, password,
                        PARAM_SUBMIT, PARAM_SUBMIT_VAL
                )
                .followRedirects(false)
                .timeout(Constants.CONNECTION_TIMEOUT_SAS)
                .method(Connection.Method.POST)
                .execute().cookies()
    }

    @WorkerThread
    @Throws(IOException::class)
    fun logout(loginCookies: Map<String, String>): Document =
            Jsoup.connect(URL_MAIN + URL_ADD_LOGOUT)
                    .userAgent(Utils.userAgent)
                    .followRedirects(false)
                    .timeout(Constants.CONNECTION_TIMEOUT_SAS)
                    .method(Connection.Method.GET)
                    .cookies(loginCookies).get()

    @WorkerThread
    @Throws(IOException::class)
    fun getGradesElements(loginCookies: Map<String, String>, semester: Semester): Elements =
            getGradesPage(loginCookies, semester)
                    //.alsoIfNot({ isLoggedIn(it) }) { throw LoginExpiredException() }
                    .select("table.isas-tabulka")
                    .select("tr")
                    .not("tr.zahlavi")

    @WorkerThread
    @Throws(IOException::class)
    fun isLoggedIn(loginCookies: Map<String, String>): Boolean =
            isLoggedIn(getGradesPage(loginCookies, Semester.AUTO))

    private fun isLoggedIn(gradesPage: Document): Boolean =
            gradesPage.select("div.isas-varovani").isEmpty() &&
                    gradesPage.select("form.isas-form").isEmpty() &&
                    !gradesPage.select("#isas-menu").isEmpty()

    @WorkerThread
    @Throws(IOException::class)
    private fun getGradesPage(loginCookies: Map<String, String>, semester: Semester): Document =
            Jsoup.connect(URL_MAIN + URL_ADD_GRADES)
                    .userAgent(Utils.userAgent)
                    .data(
                            PARAM_SEMESTER, Integer.toString(semester.value),
                            PARAM_SHOW, PARAM_SHOW_VAL_BY_DATE
                    )
                    .followRedirects(false)
                    .timeout(Constants.CONNECTION_TIMEOUT_SAS)
                    .method(Connection.Method.GET)
                    .cookies(loginCookies).get()
}

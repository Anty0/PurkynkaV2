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

import android.os.Build
import android.support.annotation.WorkerThread

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import java.io.IOException
import java.util.Locale

import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.Constants
import cz.anty.purkynka.Utils
import cz.anty.purkynka.exceptions.LoginExpiredException
import cz.anty.purkynka.grades.data.Semester

/**
 * @author anty
 */
object GradesFetcher {

    private const val LOG_TAG = "GradesFetcher"

    //private const val SCHOOL_URL = "http://www.sspbrno.cz";
    private const val MAIN_URL = "https://isas.sspbrno.cz" // TODO: use Firebase shared config
    private const val LOGIN_URL_ADD = "/prihlasit.php"
    private const val LOGIN_FIELD = "login-isas-username"
    private const val PASS_FIELD = "login-isas-password"
    private const val SUBMIT = "login-isas-send"
    private const val SUBMIT_VALUE = "isas-send"
    private const val GRADES_URL_ADD = "/prubezna-klasifikace.php"
    private const val SEMESTER = "pololeti"
    private const val SHORT_BY = "zobraz"
    private const val SHORT_BY_DATE = "datum"
    //private const val SHORT_BY_LESSONS = "predmety";
    //private const val SHORT_BY_SCORE = "hodnoceni";

    @WorkerThread
    @Throws(IOException::class)
    fun login(username: String, password: String): Map<String, String> {
        return Jsoup
                .connect(MAIN_URL + LOGIN_URL_ADD)
                .userAgent(Utils.userAgent)
                .data(LOGIN_FIELD, username, PASS_FIELD, password, SUBMIT, SUBMIT_VALUE)
                .followRedirects(false)
                .timeout(Constants.CONNECTION_TIMEOUT_SAS)
                .method(Connection.Method.POST)
                .validateTLSCertificates(true)
                .execute().cookies()
    }

    @WorkerThread
    @Throws(IOException::class)
    fun getGradesElements(loginCookies: Map<String, String>, semester: Semester): Elements {
        val gradesPage = getGradesPage(loginCookies, semester)
        if (!isLoggedIn(gradesPage)) {
            throw LoginExpiredException()
        }

        return gradesPage.select("table.isas-tabulka")
                .select("tr")
                .not("tr.zahlavi")
    }

    @WorkerThread
    @Throws(IOException::class)
    fun isLoggedIn(loginCookies: Map<String, String>): Boolean {
        return isLoggedIn(getGradesPage(loginCookies, Semester.AUTO))
    }

    private fun isLoggedIn(gradesPage: Document): Boolean {
        return gradesPage.select("div.isas-varovani").isEmpty() && gradesPage.select("form.isas-form")
                .isEmpty() && !gradesPage.select("#isas-menu").isEmpty()
    }

    @WorkerThread
    @Throws(IOException::class)
    private fun getGradesPage(loginCookies: Map<String, String>, semester: Semester): Document {
        return Jsoup
                .connect(MAIN_URL + GRADES_URL_ADD)
                .userAgent(Utils.userAgent)
                .data(SEMESTER, Integer.toString(semester.value), SHORT_BY, SHORT_BY_DATE)
                .followRedirects(false)
                .timeout(Constants.CONNECTION_TIMEOUT_SAS)
                .method(Connection.Method.GET)
                .validateTLSCertificates(true)
                .cookies(loginCookies).get()
    }
}

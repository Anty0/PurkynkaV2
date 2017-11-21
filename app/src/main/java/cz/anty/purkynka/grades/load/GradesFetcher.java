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

package cz.anty.purkynka.grades.load;

import android.os.Build;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import cz.anty.purkynka.BuildConfig;
import cz.anty.purkynka.Constants;
import cz.anty.purkynka.exceptions.LoginExpiredException;
import cz.anty.purkynka.grades.data.Semester;

/**
 * @author anty
 */
public class GradesFetcher {

    private static final String LOG_TAG = "GradesFetcher";

    //private static final String SCHOOL_URL = "http://www.sspbrno.cz";
    private static final String MAIN_URL = "https://isas.sspbrno.cz"; // TODO: use Firebase shared config
    private static final String LOGIN_URL_ADD = "/prihlasit.php";
    private static final String LOGIN_FIELD = "login-isas-username";
    private static final String PASS_FIELD = "login-isas-password";
    private static final String SUBMIT = "login-isas-send";
    private static final String SUBMIT_VALUE = "isas-send";
    private static final String GRADES_URL_ADD = "/prubezna-klasifikace.php";
    private static final String SEMESTER = "pololeti";
    private static final String SHORT_BY = "zobraz";
    private static final String SHORT_BY_DATE = "datum";
    //private static final String SHORT_BY_LESSONS = "predmety";
    //private static final String SHORT_BY_SCORE = "hodnoceni";

    public static Map<String, String> login(String username, String password, boolean loadForced) throws IOException {
        return Jsoup
                .connect(MAIN_URL + LOGIN_URL_ADD)
                .userAgent(getUserAgent(loadForced))
                .data(LOGIN_FIELD, username, PASS_FIELD, password, SUBMIT, SUBMIT_VALUE)
                .followRedirects(false)
                .timeout(Constants.CONNECTION_TIMEOUT_SAS)
                .method(Connection.Method.POST)
                .validateTLSCertificates(true)
                .execute().cookies();
    }

    public static Elements getGradesElements(Map<String, String> loginCookies, Semester semester,
                                             boolean loadForced) throws IOException {
        Document gradesPage = getGradesPage(loginCookies, semester, loadForced);
        if (!isLoggedIn(gradesPage)) {
            throw new LoginExpiredException();
        }

        return gradesPage.select("table.isas-tabulka")
                .select("tr")
                .not("tr.zahlavi");
    }

    public static boolean isLoggedIn(Map<String, String> loginCookies,
                                           boolean loadForced) throws IOException {
        return isLoggedIn(getGradesPage(loginCookies, Semester.AUTO, loadForced));
    }

    private static boolean isLoggedIn(Document gradesPage) {
        return gradesPage.select("div.isas-varovani").isEmpty() && gradesPage.select("form.isas-form")
                .isEmpty() && !gradesPage.select("#isas-menu").isEmpty();
    }

    private static Document getGradesPage(Map<String, String> loginCookies, Semester semester,
                                          boolean loadForced) throws IOException {
        return Jsoup
                .connect(MAIN_URL + GRADES_URL_ADD)
                .userAgent(getUserAgent(loadForced))
                .data(SEMESTER, Integer.toString(semester.getValue()), SHORT_BY, SHORT_BY_DATE)
                .followRedirects(false)
                .timeout(Constants.CONNECTION_TIMEOUT_SAS)
                .method(Connection.Method.GET)
                .validateTLSCertificates(true)
                .cookies(loginCookies).get();
    }

    private static String getUserAgent(boolean loadForced) {
        return String.format(Locale.ENGLISH, "Purkynka/%1$s (Android %2$s; Linux; rv:%3$d;" +
                        " loadForced:%4$b cz-cs) Gecko/20100101 Firefox/42.0",
                BuildConfig.VERSION_NAME, Build.VERSION.RELEASE, BuildConfig.VERSION_CODE, loadForced);
    }
}

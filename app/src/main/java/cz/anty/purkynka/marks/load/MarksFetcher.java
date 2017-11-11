/*
 * ApplicationPurkynka
 * Copyright (C)  2017  anty
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cz.anty.purkynka.marks.load;

import android.os.Build;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import cz.anty.purkynka.BuildConfig;
import cz.anty.purkynka.Constants;
import cz.anty.purkynka.exceptions.LoginExpiredException;
import cz.anty.purkynka.marks.data.Mark;
import cz.anty.purkynka.marks.data.Semester;

/**
 * @author anty
 */
public class MarksFetcher {

    private static final String LOG_TAG = "MarksFetcher";

    //private static final String SCHOOL_URL = "http://www.sspbrno.cz";
    private static final String MAIN_URL = "https://isas.sspbrno.cz"; // TODO: use Firebase shared config
    private static final String LOGIN_URL_ADD = "/prihlasit.php";
    private static final String LOGIN_FIELD = "login-isas-username";
    private static final String PASS_FIELD = "login-isas-password";
    private static final String SUBMIT = "login-isas-send";
    private static final String SUBMIT_VALUE = "isas-send";
    private static final String MARKS_URL_ADD = "/prubezna-klasifikace.php";
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

    public static Elements getMarksElements(Map<String, String> loginCookies, Semester semester,
                                            boolean loadForced) throws IOException {
        Document marksPage = getMarksPage(loginCookies, semester, loadForced);
        if (!isLoggedIn(marksPage)) {
            throw new LoginExpiredException();
        }

        return marksPage.select("table.isas-tabulka")
                .select("tr")
                .not("tr.zahlavi");
    }

    public static boolean isLoggedIn(Map<String, String> loginCookies,
                                           boolean loadForced) throws IOException {
        return isLoggedIn(getMarksPage(loginCookies, Semester.AUTO, loadForced));
    }

    private static boolean isLoggedIn(Document marksPage) {
        return marksPage.select("div.isas-varovani").isEmpty() && marksPage.select("form.isas-form")
                .isEmpty() && !marksPage.select("#isas-menu").isEmpty();
    }

    private static Document getMarksPage(Map<String, String> loginCookies, Semester semester,
                                         boolean loadForced) throws IOException {
        return Jsoup
                .connect(MAIN_URL + MARKS_URL_ADD)
                .userAgent(getUserAgent(loadForced))
                .data(SEMESTER, semester.getValue().toString(), SHORT_BY, SHORT_BY_DATE)
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

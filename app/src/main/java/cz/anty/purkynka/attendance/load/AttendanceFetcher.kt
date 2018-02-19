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

package cz.anty.purkynka.attendance.load

import android.support.annotation.WorkerThread
import cz.anty.purkynka.utils.*
import kotlinx.io.IOException
import org.jsoup.Jsoup
import org.jsoup.select.Elements


/**
 * @author anty
 */
object AttendanceFetcher {

    private const val URL_DEFAULT = "http://www2.sspbrno.cz/main.asp"

    private const val PARAM_TYPE = "DRUH"
    private const val PARAM_TYPE_VAL = "7"
    private const val PARAM_SUBMIT = "ODESLI"
    private const val PARAM_SUBMIT_VAL = "Vyhledat"
    private const val PARAM_QUERY = "KL_SLOVO"
    private const val PARAM_PAGE = "OD"

    @WorkerThread
    @Throws(IOException::class)
    fun getMansElements(search: String, page: Int): Elements {
        /*val urlStr = URL_DEFAULT +
                "?" + PARAM_QUERY + "=" + URLEncoder.encode(search, "Windows-1250") +
                "&" + PARAM_SUBMIT + "=" + PARAM_SUBMIT_VAL +
                "&" + PARAM_TYPE + "=" + PARAM_TYPE_VAL +
                "&" + PARAM_PAGE + "=" + page.toString()*/

        return Jsoup
                .connect(URL_DEFAULT)
                .data(
                        PARAM_QUERY, search,
                        PARAM_SUBMIT, PARAM_SUBMIT_VAL,
                        PARAM_TYPE, PARAM_TYPE_VAL,
                        PARAM_PAGE, page.toString()
                )
                .postDataCharset("Windows-1250")
                .header("Content-Type", "application/x-www-form-urlencoded")
                /*.requestBody(
                        JSON.stringify(
                                (StringSerializer to StringSerializer).map,
                                mapOf(
                                        PARAM_QUERY to search,
                                        PARAM_SUBMIT to PARAM_SUBMIT_VAL,
                                        PARAM_TYPE to PARAM_TYPE_VAL,
                                        PARAM_PAGE to page.toString()
                                )
                        )
                )
                .header("Content-Type", "application/json")*/
                .userAgent(userAgent)
                .post()
                .select("table[3]")
                .select("tr[bgcolor=#FFFFFF]")
    }
}
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

package cz.anty.purkynka.images.load

import android.support.annotation.WorkerThread
import cz.anty.purkynka.Utils
import org.json.JSONObject
import org.jsoup.Jsoup

/**
 * @author anty
 */
object ImagesFetcher {

    private const val LOG_TAG = "ImagesFetcher"

    private const val URL = "https://api.qwant.com/api/search/images"
    private const val PARAM_LOCALE = "locale"
    private const val PARAM_LOCALE_VALUE = "cs_cs"
    private const val PARAM_COUNT = "count"
    private const val PARAM_OFFSET = "offset"
    private const val PARAM_QUERY = "q"

    @WorkerThread
    fun fetch(query: String, offset: Int = 0, count: Int = 5) =
            Jsoup.connect(URL)
                    .userAgent(Utils.userAgent)
                    .data(
                            PARAM_LOCALE, PARAM_LOCALE_VALUE,
                            PARAM_COUNT, count.toString(),
                            PARAM_OFFSET, offset.toString(),
                            PARAM_QUERY, query
                    )
                    .followRedirects(false)
                    .validateTLSCertificates(true)
                    .execute().body()
                    .let { JSONObject(it) }
}
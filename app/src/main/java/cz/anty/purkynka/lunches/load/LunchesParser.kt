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

import android.content.SyncResult
import cz.anty.purkynka.lunches.data.LunchBurza
import cz.anty.purkynka.lunches.data.LunchOption
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import eu.codetopic.java.utils.substringOrNull
import eu.codetopic.java.utils.letIfNull
import eu.codetopic.java.utils.log.Log
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author anty
 */
object LunchesParser {

    private const val LOG_TAG = "LunchesParser"

    private val REGEX_LUNCH_NUMBER = Regex("^Oběd (\\d+?)$")
    private val REGEX_PIECES = Regex("^(\\d+?) ks$")
    private val REGEX_CREDIT = Regex("^([\\d,.]+?) Kč$")

    private const val COL_BURZA_LUNCH_NUMBER = 0
    private const val COL_BURZA_DATE = 1
    private const val COL_BURZA_NAME = 2
    private const val COL_BURZA_CANTEEN = 3
    private const val COL_BURZA_PIECES = 4
    private const val COL_BURZA_ORDER_URL = 5

            internal val FORMAT_DATE_SHOW =
            SimpleDateFormat("dd. MM. yyyy", Locale.getDefault())
    internal val FORMAT_DATE_SHOW_SHORT =
            SimpleDateFormat("d. M.", Locale.getDefault())
    internal val FORMAT_DATE_PARSE_LUNCH_OPTIONS =
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    internal val FORMAT_DATE_BURZA_LUNCH =
            SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH)

    private fun parseBurzaLunch(lunchElement: Element): LunchBurza =
            lunchElement.children().let { lunchElements ->
                LunchBurza(
                        lunchNumber = lunchElements[COL_BURZA_LUNCH_NUMBER].text()
                                .let { REGEX_LUNCH_NUMBER.find(it) }
                                ?.groupValues?.getOrNull(1)?.toIntOrNull()
                                ?: throw IllegalArgumentException(
                                        "Failed to parse lunchNumber from lunchElement: $lunchElement"
                                ),
                        date = try {
                            lunchElements[COL_BURZA_DATE].text()
                                    .split("\n").firstOrNull()?.trim()
                                    ?.let { FORMAT_DATE_BURZA_LUNCH.parse(it).time }
                        } catch (e: ParseException) {
                            if (e is InterruptedException) throw e
                            throw IllegalArgumentException(
                                    "Failed to parse date from lunchElement: $lunchElement", e
                            )
                        } ?: throw IllegalArgumentException(
                                "Failed to parse date from lunchElement: $lunchElement"
                        ),
                        name = lunchElements[COL_BURZA_NAME].text(),
                        canteen = lunchElements[COL_BURZA_CANTEEN].text(),
                        pieces = lunchElements[COL_BURZA_PIECES].text()
                                .let { REGEX_PIECES.find(it) }
                                ?.groupValues?.getOrNull(1)?.toIntOrNull()
                                ?: throw IllegalArgumentException(
                                        "Failed to parse pieces from lunchElement: $lunchElement"
                                ),
                        orderUrl = lunchElements[COL_BURZA_ORDER_URL]
                                .child(0).attr("onClick")
                                .substringOrNull("document.location='", "';")
                                ?: throw IllegalArgumentException(
                                        "Failed to parse orderUrl from lunchElement: $lunchElement"
                                )
                )
            }

    fun parseLunchesBurza(lunchesElements: Elements, syncResult: SyncResult? = null): List<LunchBurza> {
        Log.d(LOG_TAG, "parseLunchesBurza(lunchesElements=$lunchesElements)")
        return lunchesElements.mapNotNull map@ {
            try {
                syncResult?.apply { stats.numEntries++ }
                return@map parseBurzaLunch(it)
            } catch (e: Exception) {
                if (e is InterruptedException) throw e
                syncResult?.apply { stats.numParseExceptions++ }
                Log.w(LOG_TAG, "parseLunchesBurza", e)
                return@map null
            }
        }
    }

    fun parseLunchOption(lunchElement: Element): LunchOption {
        val name = lunchElement.child(0).child(1).text()
                .split("\n").firstOrNull()?.trim()
                ?: throw IllegalArgumentException(
                        "Failed to parse name from lunchElement: $lunchElement"
                )

        val (enabled, ordered) = when {
            lunchElement.select("a.enabled").isNotEmpty() -> true to false
            lunchElement.select("a.ordered").isNotEmpty() -> true to true
            lunchElement.select("a.disabled").firstOrNull()?.child(0)?.text()
                    ?.contains("nelze zrušit") == true -> false to true
            else -> false to false
        }

        val buttons = lunchElement.select("a.btn")

        val orderOrCancelUrl = buttons
                ?.getOrNull(0)
                ?.attr("onClick")
                ?.substringOrNull("'", "'")

        val (isInBurza, toOrFromBurzaUrl) = buttons
                ?.getOrNull(1)
                ?.let {
                    !it.text().contains("do burzy") to
                            it.attr("onClick").substringOrNull("'", "'")
                }
                ?: null to null

        return LunchOption(
                name = name,
                enabled = enabled,
                ordered = ordered,
                orderOrCancelUrl = orderOrCancelUrl,
                isInBurza = isInBurza,
                toOrFromBurzaUrl = toOrFromBurzaUrl
        )
    }

    fun parseLunchOptionsGroup(lunchElement: Element): LunchOptionsGroup {
        val lunchElements = lunchElement.children()

        val date = lunchElements.getOrNull(0)
                ?.attr("id")
                ?.replace("day-", "")
                .let { FORMAT_DATE_PARSE_LUNCH_OPTIONS.parse(it).time }

        val lunchOptions = run parseOptions@ {
            try {
                return@parseOptions lunchElements.getOrNull(1)
                        ?.select("div.jidelnicekItem")
                        ?.map { parseLunchOption(it) }
                        ?.toTypedArray()
            } catch (e: Exception) {
                if (e is InterruptedException) throw e
                Log.w(LOG_TAG, "parseLunchOptionsGroups", e)
                return@parseOptions null
            }
        }

        return LunchOptionsGroup(date, lunchOptions)
    }

    fun parseLunchOptionsGroups(lunchesElements: Elements, syncResult: SyncResult? = null): List<LunchOptionsGroup> {
        return lunchesElements.mapNotNull map@ { lunchElement ->
            try {
                syncResult?.apply { stats.numEntries++ }
                return@map parseLunchOptionsGroup(lunchElement)
            } catch (e: Exception) {
                if (e is InterruptedException) throw e
                syncResult?.apply { stats.numParseExceptions++ }
                Log.w(LOG_TAG, "parseLunchOptionsGroups", e)
                return@map null
            }
        }
    }

    fun parseCredit(elements: Element): Float =
            elements.select("span#Kredit").text()
                    .let { REGEX_CREDIT.find(it) }
                    .letIfNull { throw RuntimeException("Credit not found in page") }
                    .groupValues[1].replace(',', '.').toFloat()
}
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
import cz.anty.purkynka.lunches.data.BurzaLunch
import cz.anty.purkynka.lunches.data.LunchOption
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import eu.codetopic.java.utils.JavaExtensions.substringOrNull
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


    internal val FORMAT_DATE_SHOW =
            SimpleDateFormat("dd. MM. yyyy", Locale.getDefault())
    internal val FORMAT_DATE_SHOW_SHORT =
            SimpleDateFormat("d. M.", Locale.getDefault())
    internal val FORMAT_DATE_PARSE_LUNCH_OPTIONS =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    internal val FORMAT_DATE_BURZA_LUNCH =
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private val REGEX_LUNCH_NUMBER = Regex("^Oběd (\\d+?)$")

    private fun parseBurzaLunch(lunchElement: Element): BurzaLunch =
            lunchElement.children().let { lunchElements ->
                BurzaLunch(
                        lunchNumber = lunchElements[0].text()
                                .let { REGEX_LUNCH_NUMBER.find(it) }
                                ?.groups?.firstOrNull()?.value?.toIntOrNull()
                                ?: throw IllegalArgumentException(
                                        "Failed to parse lunchNumber from lunchElement: $lunchElement"
                                ),
                        date = try {
                            lunchElements[1].text().split("\n").firstOrNull()?.trim()
                                    ?.let { FORMAT_DATE_BURZA_LUNCH.parse(it).time }
                        } catch (e: ParseException) {
                            throw IllegalArgumentException(
                                    "Failed to parse date from lunchElement: $lunchElement", e
                            )
                        } ?: throw IllegalArgumentException(
                                "Failed to parse date from lunchElement: $lunchElement"
                        ),
                        name = lunchElements[2].text(),
                        canteen = lunchElements[3].text(),
                        pieces = lunchElements[4].text().toIntOrNull()
                                ?: throw IllegalArgumentException(
                                        "Failed to parse pieces from lunchElement: $lunchElement"
                                ),
                        orderUrl = lunchElements[5].child(0).attr("onClick")
                                .substringOrNull("document.location='", "';")
                                ?: throw IllegalArgumentException(
                                        "Failed to parse orderUrl from lunchElement: $lunchElement"
                                )
                )
            }

    fun parseBurzaLunches(lunchesElements: Elements, syncResult: SyncResult? = null): List<BurzaLunch> {
        Log.d(LOG_TAG, "parseBurzaLunches(lunchesElements=$lunchesElements)")
        return lunchesElements.mapNotNull {
            try {
                syncResult?.apply { stats.numEntries++ }
                parseBurzaLunch(it)
            } catch (e: Exception) {
                syncResult?.apply { stats.numParseExceptions++ }
                Log.w(LOG_TAG, "parseBurzaLunches", e); null
            }
        }
    }

    fun parseLunchOption(date: Long, lunchElement: Element): LunchOption {
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
                date = date,
                enabled = enabled,
                ordered = ordered,
                orderOrCancelUrl = orderOrCancelUrl,
                isInBurza = isInBurza,
                toOrFromBurzaUrl = toOrFromBurzaUrl
        )
    }

    fun parseLunchOptionsGroups(lunchesElements: Elements, syncResult: SyncResult? = null): List<LunchOptionsGroup> {
        Log.d(LOG_TAG, "parseLunchOptionsGroups(lunchesElements=$lunchesElements)")

        return lunchesElements.mapNotNull { lunchElement ->
            try {
                syncResult?.apply { stats.numEntries++ }
                val lunchElements = lunchElement.children()

                val date = lunchElements.getOrNull(0)
                        ?.attr("id")
                        ?.replace("day-", "")
                        .let { FORMAT_DATE_PARSE_LUNCH_OPTIONS.parse(it).time }

                val lunchOptions = try {
                    lunchElements.getOrNull(1)
                            ?.select("div.jidelnicekItem")
                            ?.map { parseLunchOption(date, it) }
                            ?.toTypedArray()
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "parseLunchOptionsGroups", e); null
                }

                return@mapNotNull LunchOptionsGroup(date, lunchOptions)
            } catch (e: Exception) {
                syncResult?.apply { stats.numParseExceptions++ }
                Log.w(LOG_TAG, "parseLunchOptionsGroups", e); null
            }
        }
    }
}
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

/**
 * @author anty
 */
object LunchesParser { // TODO: implement

    private const val LOG_TAG = "LunchesParser"

    /*fun parseBurzaLunches(lunches: Elements): List<BurzaLunch> {
        Log.d("Lunches", "parseBurzaLunches lunches: " + lunches.toString())

        val lunchList = ArrayList()

        for (element in lunches) {
            val lunchElements = element.children()

            val onClickText = lunchElements.get(5).child(0).attr("onClick")
            val startIndex = onClickText.indexOf("document.location='") + "document.location='".length
            val urlAdd = onClickText.substring(startIndex, onClickText.indexOf("';", startIndex))

            try {
                lunchList.add(BurzaLunch(BurzaLunch.LunchNumber.parseLunchNumber(lunchElements.get(0).text()),
                        BurzaLunch.DATE_FORMAT.parse(lunchElements.get(1).text().split("\n")[0]), lunchElements.get(2).text(), lunchElements.get(3).text(),
                        Integer.parseInt(lunchElements.get(4).text()), urlAdd))
            } catch (e: ParseException) {
                Log.d("Lunches", "parseBurzaLunches", e)
            }

        }

        return lunchList
    }

    fun parseMonthLunches(lunches: Elements): List<MonthLunchDay> {
        val lunchList = ArrayList()

        for (element in lunches) {
            val lunchElements = element.children()

            val monthLunches = ArrayList()
            val date: Date
            try {
                date = MonthLunchDay.DATE_PARSE_FORMAT.parse(lunchElements
                        .get(0).attr("id").replace("day-", ""))
            } catch (e: ParseException) {
                Log.d("Lunches", "parseMonthLunches", e)
                continue
            }

            val lunchesElements = lunchElements.get(1).select("div.jidelnicekItem")//child(0).children();
            for (lunchElement in lunchesElements) {
                val name = lunchElement.child(0).child(1).text().split("\n")[0].trim()

                var state: MonthLunch.State
                if (!lunchElement.select("a." + MonthLunch.State.ENABLED).isEmpty()) {
                    state = MonthLunch.State.ENABLED
                } else if (!lunchElement.select("a." + MonthLunch.State.ORDERED).isEmpty()) {
                    state = MonthLunch.State.ORDERED
                } else if (!lunchElement.select("a." + MonthLunch.State.DISABLED).isEmpty()) {
                    state = MonthLunch.State.DISABLED
                } else
                    state = MonthLunch.State.UNKNOWN

                if (state.equals(MonthLunch.State.DISABLED) && lunchElement.select("a." + MonthLunch.State.DISABLED)
                                .get(0).child(0).text().contains("nelze zrušit")) {
                    state = MonthLunch.State.DISABLED_ORDERED
                }

                val buttons = lunchElement.select("a.btn")
                var toBurzaUrlAdd: String? = null
                var burzaState: MonthLunch.BurzaState? = null
                var orderUrlAdd: String? = null
                when (buttons.size()) {
                    2 -> {
                        val onClickText1 = buttons.get(1).attr("onClick")
                        val startIndex1 = onClickText1.indexOf("'") + "'".length
                        toBurzaUrlAdd = onClickText1.substring(startIndex1, onClickText1.indexOf("'", startIndex1))
                        burzaState = if (buttons.get(1).text().contains(MonthLunch.BurzaState.TO_BURZA.toString()))
                            MonthLunch.BurzaState.TO_BURZA
                        else
                            MonthLunch.BurzaState.FROM_BURZA
                        val onClickText = buttons.get(0).attr("onClick")
                        val startIndex = onClickText.indexOf("'") + "'".length
                        orderUrlAdd = onClickText.substring(startIndex, onClickText.indexOf("'", startIndex))
                    }
                    1 -> {
                        val onClickText = buttons.get(0).attr("onClick")
                        val startIndex = onClickText.indexOf("'") + "'".length
                        orderUrlAdd = onClickText.substring(startIndex, onClickText.indexOf("'", startIndex))
                    }
                }

                monthLunches.add(MonthLunch(name, date, orderUrlAdd, state, toBurzaUrlAdd, burzaState))
            }

            lunchList.add(MonthLunchDay(date, monthLunches
                    .toTypedArray()))
        }

        return lunchList
    }*/
}
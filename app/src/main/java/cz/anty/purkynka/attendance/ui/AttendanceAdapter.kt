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

package cz.anty.purkynka.attendance.ui

import android.content.Context
import cz.anty.purkynka.attendance.load.AttendanceFetcher
import cz.anty.purkynka.attendance.load.AttendanceParser
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.ui.container.adapter.AutoLoadAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * @author anty
 */
class AttendanceAdapter(context: Context) : AutoLoadAdapter(context) {

    companion object {

        private const val LOG_TAG = "AttendanceAdapter"
    }

    override val startingPage: Int
        get() = 1

    var query: String = ""
        private set

    init {
        useCardView = false
    }

    fun setQuery(value: String): Job? {
        query = value
        return reset()
    }

    override fun onLoadNextPage(page: Int, editor: Editor<CustomItem>): Deferred<Boolean> {
        //val firstPage = page == startingPage
        val query = query
        return bg {
            try {
                /*if (firstPage) {
                    // TODO: add search indexing item
                }*/

                val peopleHtml = AttendanceFetcher.getMansElements(query, page)
                val people = AttendanceParser.parseMans(peopleHtml)

                editor.addAll(people.map { ManItem(it) })

                return@bg people.isNotEmpty()
            } catch (e: Exception) {
                Log.w(LOG_TAG, "onLoadNextPage(page=$page)", e)

                // TODO: add exception info item to editor

                return@bg false
            }
        }
    }
}
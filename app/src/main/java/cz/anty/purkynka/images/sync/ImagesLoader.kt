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

package cz.anty.purkynka.images.sync

import android.support.annotation.WorkerThread
import cz.anty.purkynka.images.load.ImagesFetcher
import cz.anty.purkynka.images.load.ImagesParser
import kotlinx.io.IOException

/**
 * @author anty
 */
object ImagesLoader {

    private val CACHE: MutableMap<Triple<String, Int, Int>, List<Pair<String, String>>> =
            mutableMapOf() // FIXME: limit cache size

    @WorkerThread
    @Throws(IOException::class)
    fun loadImages(query: String, offset: Int = 0, count: Int = 5): List<Pair<String, String>> =
            CACHE.getOrPut(Triple(query, offset, count)) {
                ImagesFetcher.fetchImages(query, offset, count)
                        .let { ImagesParser.extractImages(it) }
            }
}
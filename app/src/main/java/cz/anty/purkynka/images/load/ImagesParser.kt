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

import org.json.JSONObject

/**
 * @author anty
 */
object ImagesParser {

    private const val STATUS = "status"
    private const val STATUS_VAL = "success"
    private const val DATA = "data"
    private const val RESULT = "result"
    private const val ITEMS = "items"
    private const val ITEM_TITLE = "title"
    private const val ITEM_THUMBNAIL = "thumbnail"

    fun extractImages(obj: JSONObject): List<Pair<String, String>> {
        if (obj.getString(STATUS) != STATUS_VAL)
            throw IllegalArgumentException("Invalid status: ${obj.getString(STATUS)}")

        val imagesArray = obj.getJSONObject(DATA).getJSONObject(RESULT).getJSONArray(ITEMS)
        return (0 until imagesArray.length()).map {
            imagesArray.getJSONObject(it).let {
                it.getString(ITEM_TITLE) to
                        "https:${it.getString(ITEM_THUMBNAIL)}"
            }
        }
    }
}
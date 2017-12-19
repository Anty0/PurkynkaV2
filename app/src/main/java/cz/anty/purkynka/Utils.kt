/*
 * app
 * Copyright (C)   2017  anty
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

package cz.anty.purkynka

import android.graphics.Color
import android.support.annotation.ColorInt
import eu.codetopic.java.utils.JavaExtensions.join
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * @author anty
 */
object Utils {

        @ColorInt
        private val colorNeutral: Int = Color.rgb(187, 222, 251)
        private val colorSegments: Array<Array<Int>> = arrayOf(
                arrayOf(204, 255, 144),
                arrayOf(244, 255, 129),
                arrayOf(255, 209, 128),
                arrayOf(255, 158, 128),
                arrayOf(255, 138, 128)
        )

        @ColorInt
        fun colorForValue(value: Int?, size: Int): Int {
            if (value == null) return colorNeutral

            // diff r:  40  22   0   0
            // diff g:   0 -46 -51 -20
            // diff b: -15 - 1   0   0

            val fixedValue = min(max(0, value), size - 1)

            val segmentSize = size / 5F
            val segment = (fixedValue / segmentSize).toInt()
            val segmentMove = (fixedValue % segmentSize) / segmentSize

            val segmentColor = colorSegments[segment]
            if (segmentMove == 0F) return segmentColor.let { Color.rgb(it[0], it[1], it[2]) }

            val nextSegmentColor = colorSegments[segment + 1]

            val rgbColor = segmentColor.join(nextSegmentColor) { sc, nsc -> round(sc + (nsc - sc) * segmentMove).toInt() }
            return Color.rgb(rgbColor[0], rgbColor[1], rgbColor[2])
        }
}
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

package cz.anty.purkynka.grades.data

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import android.widget.Toast
import cz.anty.purkynka.R
import cz.anty.purkynka.grades.load.GradesParser
import eu.codetopic.utils.ui.container.items.custom.CardViewWrapper
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.items.custom.CustomItemWrapper
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import proguard.annotation.Keep
import proguard.annotation.KeepClassMemberNames
import proguard.annotation.KeepClassMembers
import proguard.annotation.KeepName
import java.util.*

/**
 * @author anty
 */
/*@Keep
@KeepName
@KeepClassMembers
@KeepClassMemberNames*/ // TODO: are this keeps really required?
@Serializable
data class Grade(val id: Int, val date: Long, val subjectShort: String, val subjectLong: String, val valueToShow: String,
                 val value: Float, val type: String, val weight: Int, val note: String, val teacher: String) {

    @Transient
    val dateStr: String get() = GradesParser.GRADE_DATE_FORMAT.format(Date(date))

    override fun hashCode() = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Grade

        if (id != other.id) return false

        return true
    }

    infix fun differentTo(other: Grade) = id != other.id
            || date != other.date
            || subjectShort != other.subjectShort
            || subjectLong != other.subjectLong
            || valueToShow != other.valueToShow
            || value != other.value
            || type != other.type
            || weight != other.weight
            || note != other.note
            || teacher != other.teacher
}
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
import android.view.View
import cz.anty.purkynka.R
import cz.anty.purkynka.attendance.data.Man
import cz.anty.purkynka.attendance.data.Man.Companion.lastEnterStr
import eu.codetopic.java.utils.*
import eu.codetopic.java.utils.fillToLen
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.android.synthetic.main.item_man.*

/**
 * @author anty
 */
class ManItem(val base: Man) : CustomItem() {

    companion object {

        private const val LOG_TAG = "ManItem"
    }

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {
        holder.txtClassId.apply {
            val classId = base.classId
            textSize = if (classId.length <= 4) 28F else 18F
            text = classId.fillToLen(4, Anchor.LEFT)
        }

        holder.imgIsInSchoolYes.visibility = if (base.isInSchool == true) View.VISIBLE else View.GONE
        holder.imgIsInSchoolNo.visibility = if (base.isInSchool == false) View.VISIBLE else View.GONE
        holder.imgIsInSchoolUnknown.visibility = if (base.isInSchool == null) View.VISIBLE else View.GONE

        holder.txtName.text = base.name

        holder.txtLastEnterDate.text = base.lastEnterStr
    }

    override fun getLayoutResId(context: Context): Int = R.layout.item_man

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ManItem

        if (base != other.base) return false

        return true
    }

    override fun hashCode(): Int {
        return base.hashCode()
    }

    override fun toString(): String {
        return "ManItem(base=$base)"
    }
}
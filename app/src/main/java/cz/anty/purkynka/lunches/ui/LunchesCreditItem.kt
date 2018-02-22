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

package cz.anty.purkynka.lunches.ui

import android.content.Context
import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import cz.anty.purkynka.R
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import kotlinx.android.synthetic.main.item_lunches_credit.*
import java.text.DecimalFormat

/**
 * @author anty
 */
class LunchesCreditItem(val credit: Float) : CustomItem() {

    companion object {

        private const val LOG_TAG = "LunchesCreditItem"

        private val FORMAT_CREDIT = DecimalFormat("#.##")
    }

    override fun onBindViewHolder(holder: ViewHolder, itemPosition: Int) {
        holder.txtCredit.text = SpannableStringBuilder().apply {
            append(holder.context.getText(R.string.text_view_credit))
            append(
                    SpannableStringBuilder().apply {
                        val colorSpan = ForegroundColorSpan(
                                ContextCompat.getColor(
                                        holder.context,
                                        when {
                                            credit.isNaN() -> R.color.materialBlue
                                            credit < 60 -> R.color.materialRed
                                            credit < 90 -> R.color.materialYellow
                                            else -> R.color.materialGreen
                                        }
                                )
                        )

                        setSpan(colorSpan, 0, 0, Spanned.SPAN_MARK_MARK)
                        append(
                                if (credit.isNaN()) "?"
                                else FORMAT_CREDIT.format(credit)
                        )
                        append(",- Kč")
                        setSpan(colorSpan, 0, this.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
            )
        }
    }

    override fun getItemLayoutResId(context: Context): Int = R.layout.item_lunches_credit

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
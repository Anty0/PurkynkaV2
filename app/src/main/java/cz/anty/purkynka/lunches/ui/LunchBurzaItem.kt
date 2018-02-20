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
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import cz.anty.purkynka.R
import cz.anty.purkynka.lunches.data.LunchBurza
import cz.anty.purkynka.lunches.data.LunchBurza.Companion.dateStrShort
import eu.codetopic.java.utils.Anchor
import eu.codetopic.java.utils.fillToLen
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.baseActivity
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import kotlinx.android.synthetic.main.item_lunch_burza.*
import org.jetbrains.anko.textColorResource
import java.util.*

/**
 * @author anty
 */
class LunchBurzaItem(val accountId: String, val base: LunchBurza) : CustomItem() {

    companion object {

        private const val LOG_TAG = "LunchBurzaItem"
    }

    override fun onBindViewHolder(holder: ViewHolder, itemPosition: Int) {
        holder.txtDay.apply {
            text = Calendar.getInstance()
                    .apply { timeInMillis = base.date }
                    .get(Calendar.DAY_OF_WEEK)
                    .let {
                        when (it) {
                            Calendar.SUNDAY -> R.string.txt_day_short_sunday
                            Calendar.MONDAY -> R.string.txt_day_short_monday
                            Calendar.TUESDAY -> R.string.txt_day_short_tuesday
                            Calendar.WEDNESDAY -> R.string.txt_day_short_wednesday
                            Calendar.THURSDAY -> R.string.txt_day_short_thursday
                            Calendar.FRIDAY -> R.string.txt_day_short_friday
                            Calendar.SATURDAY -> R.string.txt_day_short_saturday
                            else -> R.string.txt_day_short_unknown
                        }
                    }
                    .let { holder.context.getText(it) }
            textColorResource = base.pieces.let { pieces ->
                when {
                    pieces >= 5 -> R.color.materialGreen
                    pieces == 4 -> R.color.materialYellow
                    pieces == 3 -> R.color.materialOrange
                    pieces == 2 -> R.color.materialLightRed
                    pieces == 1 -> R.color.materialRed
                    else -> R.color.materialBlue
                }
            }
        }

        holder.txtDate.text = base.dateStrShort.fillToLen(7, Anchor.RIGHT)

        holder.txtName.text = base.name

        holder.txtLunchNumber.text = SpannableStringBuilder().apply {
            append(holder.context.getFormattedText(
                    R.string.text_view_lunches_burza_lunch_pieces,
                    base.pieces
            ))
            append(" - ")
            append(holder.context.getFormattedText(
                    R.string.text_view_lunches_lunch_number,
                    base.lunchNumber
            ))
        }

        if (itemPosition != NO_POSITION) { // detects usage in header
            holder.boxClickTarget.setOnClickListener {
                val context = holder.context
                val options = context.baseActivity?.let {
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            it,
                            holder.boxClickTarget,
                            context.getString(R.string.id_transition_lunch_burza_item)
                    )
                }

                if (options == null) Log.e(LOG_TAG, "Can't start LunchBurzaActivity " +
                        "with transition: Cannot find Activity in context hierarchy")

                ContextCompat.startActivity(
                        context,
                        LunchBurzaActivity.getStartIntent(context, accountId, base),
                        options?.toBundle()
                )
            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getItemLayoutResId(context: Context): Int = R.layout.item_lunch_burza

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LunchBurzaItem

        if (base != other.base) return false

        return true
    }

    override fun hashCode(): Int {
        return base.hashCode()
    }

    override fun toString(): String {
        return "LunchBurzaItem(accountId=$accountId, base=$base)"
    }
}
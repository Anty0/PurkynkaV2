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
import cz.anty.purkynka.R
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import cz.anty.purkynka.lunches.data.LunchOptionsGroup.Companion.dateStrShort
import eu.codetopic.java.utils.Anchor
import eu.codetopic.java.utils.fillToLen
import eu.codetopic.java.utils.letIfNull
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.baseActivity
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.getFormattedQuantityText
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import kotlinx.android.synthetic.main.item_lunch_options_group.*
import org.jetbrains.anko.textColorResource
import java.util.*

/**
 * @author anty
 */
class LunchOptionsGroupItem(val accountId: String, val base: LunchOptionsGroup) : CustomItem() {

    companion object {

        private const val LOG_TAG = "LunchOptionsGroupItem"
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
            textColorResource = when {
                // Lunch is ordered
                base.orderedOption != null -> R.color.materialGreen
                // TODO: If lunch is not ordered (and can't be ordered) and is available in burza use materialRed
                // Lunch is not ordered and can't be ordered
                base.options?.all { !it.enabled } != false -> R.color.materialBlue
                // Lunch is not ordered, but still can be ordered
                else -> R.color.materialOrange
            }
        }

        holder.txtDate.text = base.dateStrShort.fillToLen(7, Anchor.RIGHT)

        val (orderedIndex, orderedLunch) = base.orderedOption ?: null to null

        holder.txtName.text = orderedLunch?.name
                ?: holder.context.getText(R.string.text_view_lunches_no_lunch_ordered)

        holder.txtOrderedIndex.text = orderedIndex
                ?.let { it + 1 }
                ?.let {
                    holder.context.getFormattedText(R.string.text_view_lunches_ordered_index, it)
                }
                ?:
                run {
                    base.options
                            ?.filter { it.enabled }
                            ?.count()
                            .letIfNull { 0 }
                            .let {
                                holder.context.getFormattedQuantityText(
                                        R.plurals.text_view_lunches_available_count,
                                        it, it
                                )
                            }
                }

        if (itemPosition != NO_POSITION) { // detects usage in header
            holder.boxClickTarget.setOnClickListener {
                val context = holder.context
                val options = context.baseActivity?.let {
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            it,
                            holder.boxClickTarget,
                            context.getString(R.string.id_transition_lunch_options_group_item)
                    )
                }

                if (options == null) Log.e(LOG_TAG, "Can't start LunchOptionsGroupActivity " +
                        "with transition: Cannot find Activity in context hierarchy")

                ContextCompat.startActivity(
                        context,
                        LunchOptionsGroupActivity.getStartIntent(context, accountId, base),
                        options?.toBundle()
                )
            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getItemLayoutResId(context: Context): Int = R.layout.item_lunch_options_group

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LunchOptionsGroupItem

        if (base != other.base) return false

        return true
    }

    override fun hashCode(): Int {
        return base.hashCode()
    }

    override fun toString(): String {
        return "LunchOptionsGroupItem(accountId=$accountId, base=$base)"
    }
}
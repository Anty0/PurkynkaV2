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

package cz.anty.purkynka.grades.ui

import android.content.Context
import android.graphics.Typeface
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import android.view.View
import cz.anty.purkynka.R
import cz.anty.purkynka.grades.data.Subject
import cz.anty.purkynka.grades.data.Subject.Companion.average
import cz.anty.purkynka.grades.data.Subject.Companion.averageColor
import eu.codetopic.java.utils.Anchor
import eu.codetopic.java.utils.fillToLen
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.java.utils.format
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.baseActivity
import eu.codetopic.utils.getFormattedQuantityText
import eu.codetopic.utils.ui.container.items.custom.CustomItemRemoteViewHolder
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.android.synthetic.main.item_subject.*

/**
 * @author anty
 */
class SubjectItem(val base: Subject, val isBad: Boolean,
                  val changes: Map<Int, List<String>> = emptyMap()): CustomItem() { // TODO: use changes

    companion object {

        private const val LOG_TAG = "SubjectItem"
    }

    private val average: Double = base.average
    private val averageColor: Int = base.averageColor

    val isChnaged get() = changes.isNotEmpty()

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {
        holder.txtNameShort.apply {
            setTextColor(averageColor)
            text = base.shortName.fillToLen(4, Anchor.LEFT)
        }

        holder.txtAverage.apply {
            setTypeface(null, if (isBad) Typeface.BOLD else Typeface.NORMAL)
            //setTextColor(averageColor)
            text = average.format(2)
        }

        holder.txtNameLong.text = base.fullName

        val gradesCount = base.grades.size
        holder.txtGradesCount.text = holder.context
                .getFormattedQuantityText(
                        R.plurals.text_view_grades_count,
                        gradesCount, gradesCount
                )

        holder.boxColoredBackground.setBackgroundResource(
                if (!isChnaged) android.R.color.transparent
                else R.color.cardview_dark_background
        )

        if (itemPosition != NO_POSITION) { // detects usage in header
            holder.boxClickTarget.setOnClickListener {
                val context = holder.context
                val options = context.baseActivity?.let {
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            it,
                            holder.boxColoredBackground,
                            context.getString(R.string.id_transition_subject_item)
                    )
                }

                if (options == null) Log.e(LOG_TAG, "Can't start SubjectActivity " +
                        "with transition: Cannot find Activity in context hierarchy")

                ContextCompat.startActivity(
                        context,
                        SubjectActivity.getStartIntent(context, base, isBad, changes),
                        options?.toBundle()
                )
            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getLayoutResId(context: Context): Int = R.layout.item_subject

    override fun onBindRemoteViewHolder(holder: CustomItemRemoteViewHolder, itemPosition: Int) {
        holder.itemView.setTextColor(R.id.txtNameShort, averageColor)
        holder.itemView.setTextViewText(
                R.id.txtNameShort,
                base.shortName.fillToLen(4, Anchor.LEFT)
        )

        holder.itemView.setViewVisibility(
                R.id.txtAverageBold,
                if (isBad) View.VISIBLE else View.GONE
        )
        holder.itemView.setViewVisibility(
                R.id.txtAverageNormal,
                if (!isBad) View.VISIBLE else View.GONE
        )
        val averageText = average.format(2)
        holder.itemView.setTextViewText(R.id.txtAverageBold, averageText)
        holder.itemView.setTextViewText(R.id.txtAverageNormal, averageText)
    }

    override fun getRemoteLayoutResId(context: Context): Int = R.layout.item_subject_widget

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubjectItem

        if (base != other.base) return false

        return true
    }

    override fun hashCode(): Int = base.hashCode()

    override fun toString(): String = "SubjectItem(base=$base)"
}
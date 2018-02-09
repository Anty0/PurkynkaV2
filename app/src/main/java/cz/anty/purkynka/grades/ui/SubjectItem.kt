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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.R
import cz.anty.purkynka.Utils.colorForValue
import cz.anty.purkynka.grades.data.Subject
import cz.anty.purkynka.grades.data.Subject.Companion.average
import cz.anty.purkynka.grades.data.Subject.Companion.averageColor
import eu.codetopic.java.utils.JavaExtensions
import eu.codetopic.java.utils.JavaExtensions.fillToLen
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.java.utils.JavaExtensions.format
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.baseActivity
import eu.codetopic.utils.AndroidExtensions.getFormattedQuantityText
import kotlinx.android.synthetic.main.item_subject.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author anty
 */
@Serializable
class SubjectItem(val base: Subject,
                  val changes: Map<Int, List<String>> = emptyMap()): CustomItem() { // TODO: use changes

    companion object {

        private const val LOG_TAG = "SubjectItem"
    }

    @Transient
    val isChnaged get() = changes.isNotEmpty()

    override fun onBindViewHolder(holder: ViewHolder, itemPosition: Int) {
        holder.txtNameShort.text = base.shortName.fillToLen(4, JavaExtensions.Anchor.LEFT)

        holder.txtAverage.apply {
            setTextColor(base.averageColor)
            text = base.average.format(2)
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
                /*Toast.makeText(
                        holder.context,
                        "onClick(position$itemPosition, subject=$this)",
                        Toast.LENGTH_LONG
                ).show()*/

                val context = holder.context
                val options = context.baseActivity?.let {
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            it,
                            holder.boxColoredBackground,
                            context.getString(R.string.id_transition_subject_item)
                    ).toBundle()
                }

                if (options == null) Log.w(LOG_TAG, "Can't start SubjectActivity " +
                        "with transition: Cannot find Activity in context hierarchy")

                ContextCompat.startActivity(
                        context,
                        SubjectActivity.getStartIntent(context, this),
                        options
                )
            }
        }
    }

    override fun getItemLayoutResId(context: Context): Int = R.layout.item_subject

    //override fun getWrappers(context: Context): Array<CustomItemWrapper> = CardViewWrapper.WRAPPER

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
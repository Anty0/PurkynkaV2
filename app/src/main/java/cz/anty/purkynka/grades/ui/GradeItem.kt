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
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Grade.Companion.valueColor
import eu.codetopic.java.utils.Anchor
import eu.codetopic.java.utils.fillToLen
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.baseActivity
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.items.custom.CustomItemRemoteViewHolder
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.android.synthetic.main.item_grade.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author anty
 */
class GradeItem(val base: Grade, val isBad: Boolean, val showSubject: Boolean = true,
                val changes: List<String>? = null): CustomItem() { // TODO: use changes

    companion object {

        private const val LOG_TAG = "GradeItem"
    }

    private val valueColor: Int = base.valueColor

    val isNew
        get() = changes?.isEmpty() == true

    val isChanged
        get() = changes?.isNotEmpty() == true

    val hasChnges
        get() = changes != null

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {
        holder.txtSubject.apply {
            setTextColor(valueColor)
            visibility = if (showSubject) View.VISIBLE else View.GONE
            text = base.subjectShort.fillToLen(4, Anchor.LEFT)
        }

        holder.txtGrade.apply {
            setTypeface(null, if (isBad) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(
                    if (!showSubject) valueColor
                    else ContextCompat.getColor(holder.context, R.color.materialWhite)
            )
            text = base.valueToShow
        }

        holder.txtWeight.apply {
            setTypeface(null, if (base.weight >= 3) Typeface.BOLD else Typeface.NORMAL)
            text = base.weight.toString()
        }

        holder.txtNote.text = base.note

        holder.txtTeacher.text = base.teacher

        holder.boxColoredBackground.setBackgroundResource(
                if (!hasChnges) android.R.color.transparent
                else R.color.cardview_dark_background
        )

        if (itemPosition != NO_POSITION) { // detects usage in header
            holder.boxClickTarget.setOnClickListener {
                val context = holder.context
                val options = context.baseActivity?.let {
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            it,
                            holder.boxColoredBackground,
                            context.getString(R.string.id_transition_grade_item)
                    )
                }

                if (options == null) Log.e(LOG_TAG, "Can't start GradeActivity " +
                        "with transition: Cannot find Activity in context hierarchy")

                ContextCompat.startActivity(
                        context,
                        GradeActivity.getStartIntent(context, base, showSubject, isBad, changes),
                        options?.toBundle()
                )
            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getLayoutResId(context: Context) = R.layout.item_grade

    override fun onBindRemoteViewHolder(holder: CustomItemRemoteViewHolder, itemPosition: Int) {
        holder.itemView.setTextColor(R.id.txtSubject, valueColor)
        holder.itemView.setViewVisibility(
                R.id.txtSubject,
                if (showSubject) View.VISIBLE else View.GONE
        )
        holder.itemView.setTextViewText(
                R.id.txtSubject,
                base.subjectShort.fillToLen(4, Anchor.LEFT)
        )

        holder.itemView.setViewVisibility(
                R.id.txtGradeBold,
                if (isBad) View.VISIBLE else View.GONE
        )
        holder.itemView.setViewVisibility(
                R.id.txtGradeNormal,
                if (!isBad) View.VISIBLE else View.GONE
        )
        val txtGradeColor =
                if (!showSubject) valueColor
                else ContextCompat.getColor(holder.context, R.color.materialWhite)
        holder.itemView.setTextColor(R.id.txtGradeBold, txtGradeColor)
        holder.itemView.setTextColor(R.id.txtGradeNormal, txtGradeColor)
        holder.itemView.setTextViewText(R.id.txtGradeBold, base.valueToShow)
        holder.itemView.setTextViewText(R.id.txtGradeNormal, base.valueToShow)

        holder.itemView.setViewVisibility(
                R.id.txtWeightBold,
                if (base.weight >= 3) View.VISIBLE else View.GONE
        )
        holder.itemView.setViewVisibility(
                R.id.txtWeightNormal,
                if (base.weight < 3) View.VISIBLE else View.GONE
        )
        holder.itemView.setTextViewText(R.id.txtWeightBold, base.weight.toString())
        holder.itemView.setTextViewText(R.id.txtWeightNormal, base.weight.toString())

        holder.itemView.setInt(
                R.id.boxColoredBackground,
                "setBackgroundColor",
                ContextCompat.getColor(
                        holder.context,
                        if (!hasChnges) android.R.color.transparent
                        else R.color.navigationBarColorGrades
                )
        )
    }

    override fun getRemoteLayoutResId(context: Context): Int = R.layout.item_grade_widget

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GradeItem

        if (base != other.base) return false
        if (showSubject != other.showSubject) return false

        return true
    }

    override fun hashCode(): Int {
        var result = base.hashCode()
        result = 31 * result + showSubject.hashCode()
        return result
    }

    override fun toString(): String {
        return "GradeItem(base=$base, showSubject=$showSubject)"
    }
}
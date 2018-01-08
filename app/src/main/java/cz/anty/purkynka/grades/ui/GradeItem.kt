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
import android.widget.Toast
import cz.anty.purkynka.R
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.Utils.colorForValue
import eu.codetopic.java.utils.JavaExtensions
import eu.codetopic.java.utils.JavaExtensions.fillToLen
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.baseActivity
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import kotlinx.android.synthetic.main.item_grade.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @author anty
 */
@Serializable
class GradeItem(val base: Grade, val showSubject: Boolean = true,
                val changes: List<String>? = null): CustomItem() { // TODO: use changes

    companion object {

        private const val LOG_TAG = "GradeItem"
    }

    @Transient
    val isNew = changes?.isEmpty() == true

    @Transient
    val isChanged = changes?.isNotEmpty() == true

    @Transient
    val hasChnges = changes != null

    override fun onBindViewHolder(holder: CustomItem.ViewHolder, itemPosition: Int) {
        val textStyle = if (base.weight >= 3) Typeface.BOLD else Typeface.NORMAL

        holder.txtSubject.apply {
            visibility = if (showSubject) View.VISIBLE else View.GONE
            text = base.subjectShort.fillToLen(4, JavaExtensions.Anchor.LEFT)
        }

        holder.txtGrade.apply {
            setTypeface(null, textStyle)
            setTextColor(colorForValue(
                    base.value.toInt().let {
                        if (it == 0) null else it - 1
                    },
                    5
            ))
            text = base.valueToShow
        }

        holder.txtWeight.apply {
            setTypeface(null, textStyle)
            text = base.weight.toString()
        }

        holder.txtNote.text = base.note

        holder.txtTeacher.text = base.teacher

        holder.boxColoredBackground.setBackgroundResource(
                if (!hasChnges) android.R.color.transparent
                else R.color.abc_color_highlight_material
        )

        if (itemPosition != NO_POSITION) { // detects usage in header
            holder.boxClickTarget.setOnClickListener {
                /*Toast.makeText(
                        holder.context,
                        "onClick(position$itemPosition, grade=$base)",
                        Toast.LENGTH_LONG
                ).show()*/

                val context = holder.context
                val options = context.baseActivity?.let {
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            it,
                            holder.boxColoredBackground,
                            context.getString(R.string.id_transition_grade_item)
                    )
                }

                if (options == null) Log.w(LOG_TAG, "Can't start GradeActivity " +
                        "with transition: Cannot find Activity in context hierarchy")

                ContextCompat.startActivity(
                        context,
                        GradeActivity.getStartIntent(context, this),
                        options?.toBundle()
                )
            }
        }
    }

    override fun getItemLayoutResId(context: Context) = R.layout.item_grade

    //override fun getWrappers(context: Context): Array<CustomItemWrapper> = CardViewWrapper.WRAPPER

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
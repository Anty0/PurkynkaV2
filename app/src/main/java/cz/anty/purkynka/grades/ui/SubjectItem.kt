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
import android.widget.TextView
import android.widget.Toast
import cz.anty.purkynka.R
import cz.anty.purkynka.grades.data.Subject
import eu.codetopic.utils.ui.container.items.custom.CardViewWrapper
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.items.custom.CustomItemWrapper

/**
 * @author anty
 */
class SubjectItem(private val base: Subject): CustomItem() {

    override fun onBindViewHolder(holder: ViewHolder, itemPosition: Int) {
        val nameView: TextView = holder.itemView.findViewById(R.id.text_view_subject_name)

        nameView.text = base.fullName

        // TODO: complete layout

        holder.topParentHolder.itemView.setOnClickListener {
            Toast.makeText(
                    holder.context,
                    "onClick(position$itemPosition, subject=$this)",
                    Toast.LENGTH_LONG
            ).show()
            // TODO: show activity of subject grades
        }
    }

    override fun getItemLayoutResId(context: Context?): Int = R.layout.item_subject

    override fun getWrappers(context: Context?): Array<CustomItemWrapper> = CardViewWrapper.WRAPPER
}
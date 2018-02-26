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

package cz.anty.purkynka.update.ui

import android.content.Context
import cz.anty.purkynka.R
import cz.anty.purkynka.update.VersionInfo
import cz.anty.purkynka.update.inflateChangesLayout
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.android.synthetic.main.item_version_changes.*

/**
 * @author anty
 */
class VersionChangesItem(val versionCode: Int, val versionInfo: VersionInfo,
                         val highlight: Boolean = false) : CustomItem() {

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {
        holder.txtVersion.text = holder.context.getFormattedText(
                R.string.item_version_name,
                versionInfo.name
        )

        holder.boxChanges.apply {
            removeAllViews()
            versionInfo.inflateChangesLayout(this, 5)
        }

        holder.boxColoredBackground.setBackgroundResource(
                if (!highlight) android.R.color.transparent
                else R.color.cardview_dark_background
        )

        if (itemPosition != NO_POSITION) {
            holder.boxClickTarget.setOnClickListener {
                VersionChangesActivity.start(holder.context, versionCode)
            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getLayoutResId(context: Context): Int = R.layout.item_version_changes
}
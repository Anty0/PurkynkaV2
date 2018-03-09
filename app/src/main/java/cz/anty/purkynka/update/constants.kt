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

package cz.anty.purkynka.update

import android.content.Context
import android.support.annotation.ArrayRes
import android.text.SpannableStringBuilder
import android.view.ViewGroup
import cz.anty.purkynka.R
import kotlinx.android.synthetic.main.item_version_changes_line.view.*
import kotlinx.collections.immutable.immutableMapOf
import org.jetbrains.anko.layoutInflater

/**
 * @author anty
 */

val CHANGELOG_MAP = immutableMapOf(
        1 to VersionInfo("v5.0.0", R.array.version_changes_1),
        2 to VersionInfo("v5.0.1", R.array.version_changes_2),
        3 to VersionInfo("v5.0.2", R.array.version_changes_3),
        4 to VersionInfo("v5.0.3", R.array.version_changes_4),
        5 to VersionInfo("v5.0.4", R.array.version_changes_5),
        6 to VersionInfo("v5.0.5", R.array.version_changes_6)
)

data class VersionInfo(val name: String, @get:ArrayRes @param:ArrayRes val changesTextRes: Int)

fun VersionInfo.getChangesText(context: Context, limit: Int = -1): CharSequence {
    val prefix = context.getText(R.string.version_changes_line_prefix)
    return context.resources.getTextArray(changesTextRes).joinTo(
            buffer = SpannableStringBuilder(),
            separator = SpannableStringBuilder("\n").append(prefix),
            prefix = prefix,
            limit = limit
    )
}

fun VersionInfo.inflateChangesLayout(root: ViewGroup, limit: Int = -1) {
    val inflater = root.context.layoutInflater
    root.context.resources
            .getTextArray(changesTextRes)
            .let {
                if (limit != -1 && it.size > limit)
                    it.take(limit) + "..."
                else it.toList()
            }
            .forEach {
                inflater.inflate(
                        R.layout.item_version_changes_line,
                        root,
                        false
                ).apply {
                    txtChange.text = it
                    root.addView(this)
                }
            }
}
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
import android.content.Intent
import android.os.Bundle
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import cz.anty.purkynka.R
import cz.anty.purkynka.update.CHANGELOG_MAP
import eu.codetopic.utils.getIconics
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.container.recycler.Recycler

/**
 * @author anty
 */
class ChangelogActivity : ModularActivity(ToolbarModule(), BackButtonModule()) {

    companion object {

        private const val LOG_TAG = "ChangelogActivity"

        fun getIntent(context: Context): Intent =
                Intent(context, ChangelogActivity::class.java)

        fun start(context: Context) =
                context.startActivity(getIntent(context))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Recycler.inflate().withItemDivider().on(this)
                .setEmptyImage(getIconics(GoogleMaterial.Icon.gmd_track_changes).sizeDp(72))
                .setEmptyText(R.string.empty_view_text_changelog)
                .setSmallEmptyText(R.string.empty_view_text_small_changelog)
                .setAdapter(
                        CHANGELOG_MAP.entries.reversed().map {
                            val (code, info) = it
                            return@map VersionChangesItem(code, info)
                        }
                )
    }
}
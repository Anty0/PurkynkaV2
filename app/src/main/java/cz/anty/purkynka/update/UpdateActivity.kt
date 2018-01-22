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
import android.content.Intent
import android.os.Bundle
import cz.anty.purkynka.R
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions

/**
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class UpdateActivity : ModularActivity(ToolbarModule(), BackButtonModule()) { // TODO: implement

    companion object {

        fun generateIntent(context: Context): Intent =
                Intent(context, UpdateActivity::class.java)

        fun start(context: Context) =
                context.startActivity(generateIntent(context))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)
    }

    /*new Intent(Intent.ACTION_VIEW) // Install update intent
        .setDataAndType(Uri.fromFile(new File(path)), "application/vnd.android.package-archive")
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);*/
}
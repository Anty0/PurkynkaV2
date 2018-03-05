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

package cz.anty.purkynka.easter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import cz.anty.purkynka.R
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.TransitionBackButtonModule

/**
 * @author anty
 */
class EasterEggActivity : ModularActivity(TransitionBackButtonModule()) {

    companion object {

        private const val LOG_TAG = "EasterEggActivity"

        fun getStartIntent(context: Context) =
                Intent(context, EasterEggActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_easter_egg)
    }

    override fun finishAfterTransition() {
        // fixes bug in Android M (5), that crashes application
        //  if shared element is missing in previous activity.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) super.finishAfterTransition()
        else finish()
    }
}
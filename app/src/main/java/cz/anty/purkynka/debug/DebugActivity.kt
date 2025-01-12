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

package cz.anty.purkynka.debug

import eu.codetopic.utils.debug.BaseDebugActivity
import eu.codetopic.utils.ui.container.items.custom.CustomItem

/**
 * Created by anty on 6/16/17.
 *
 * @author anty
 */
class DebugActivity : BaseDebugActivity() {

    override fun prepareDebugItems(items: MutableList<CustomItem>) {
        // add here App debug items
        items.add(AppDebugItem())

        super.prepareDebugItems(items)
    }
}
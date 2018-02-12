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

package cz.anty.purkynka.lunches

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.anty.purkynka.R
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import proguard.annotation.KeepName

/**
 * @author anty
 */
@KeepName
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class LunchesBurzaWatcherFragment : NavigationFragment(), TitleProvider, ThemeProvider {

    override val title: CharSequence
        get() = getText(R.string.title_fragment_lunches_burza_watcher)
    override val themeId: Int
        get() = R.style.AppTheme_Lunches

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
                                     savedInstanceState: Bundle?): View? {
        val themedContext = ContextThemeWrapper(inflater.context, themeId)
        val themedInflater = inflater.cloneInContext(themedContext)
        //val view = themedInflater.inflate(R.layout.fragment_, container, false)
        // TODO: implement
        //return view
        return super.onCreateContentView(inflater, container, savedInstanceState)
    }
}
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

package cz.anty.purkynka.settings

import android.content.Context
import android.content.SharedPreferences
import cz.anty.purkynka.utils.FILE_NAME_APP_PREFERENCES
import cz.anty.purkynka.utils.SHOW_TRY_SWIPE_ITEM
import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.preference.BooleanPreference
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.provider.ISharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.ContentProviderSharedPreferences
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs
import eu.codetopic.utils.data.preferences.support.VersionedContentProviderPreferences

/**
 * @author anty
 */
class AppPreferences private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, AppPreferencesProvider.AUTHORITY)) {

    companion object : PreferencesCompanionObject<AppPreferences>(
            AppPreferences.LOG_TAG,
            ::AppPreferences,
            ::Getter
    ) {

        private const val LOG_TAG = "AppPreferences"
        internal const val SAVE_VERSION = 0

        @Suppress("UNUSED_PARAMETER")
        internal fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
            // This function will be executed by provider in provider process
            when (from) {
                -1 -> {
                    // First start, nothing to do
                }
            } // No more versions yet
        }
    }

    var showTrySwipeItem by BooleanPreference(
            key = SHOW_TRY_SWIPE_ITEM,
            provider = accessProvider,
            defaultValue = true
    )

    private class Getter : PreferencesGetterAbs<AppPreferences>() {

        override fun get(): AppPreferences = instance

        override val dataClass: Class<out AppPreferences>
            get() = AppPreferences::class.java
    }
}

class AppPreferencesProvider : VersionedContentProviderPreferences<SharedPreferences>(AUTHORITY, AppPreferences.SAVE_VERSION) {

    companion object {
        const val AUTHORITY = "cz.anty.purkynka.preferences"
    }

    override fun onPreparePreferencesProvider(): ISharedPreferencesProvider<SharedPreferences> {
        return BasicSharedPreferencesProvider(context, FILE_NAME_APP_PREFERENCES, Context.MODE_PRIVATE)
    }

    override fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
        AppPreferences.onUpgrade(editor, from, to)
    }
}
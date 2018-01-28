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

package cz.anty.purkynka.grades.save

import android.content.Context
import android.content.SharedPreferences
import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.extension.LoginDataExtension
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.provider.SecureSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.ContentProviderSharedPreferences
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs
import eu.codetopic.utils.data.preferences.support.SecurePreferences

/**
 * @author anty
 */
class GradesLoginData private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, GradesLoginProvider.AUTHORITY)) {

    companion object : PreferencesCompanionObject<GradesLoginData>(GradesLoginData.LOG_TAG,
            ::GradesLoginData, GradesLoginData::Getter) {

        private const val LOG_TAG = "GradesLoginData"
        internal const val SAVE_VERSION = 0

        val loginData get() = instance.loginData

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

    val loginData: LoginDataExtension<ContentProviderSharedPreferences>
            by lazy { LoginDataExtension(accessProvider) }

    private class Getter : PreferencesGetterAbs<GradesLoginData>() {

        override fun get() = instance

        override val dataClass: Class<out GradesLoginData>
            get() = GradesLoginData::class.java
    }
}
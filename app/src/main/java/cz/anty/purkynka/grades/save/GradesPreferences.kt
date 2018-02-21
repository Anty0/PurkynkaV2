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

package cz.anty.purkynka.grades.save

import android.content.Context
import android.content.SharedPreferences
import cz.anty.purkynka.utils.*

import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.preference.FloatPreference
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.support.*

/**
 * @author anty
 */
class GradesPreferences private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, GradesPreferencesProvider.AUTHORITY)) {

    companion object : PreferencesCompanionObject<GradesPreferences>(
            GradesPreferences.LOG_TAG,
            ::GradesPreferences,
            ::Getter
    ) {

        private const val LOG_TAG = "GradesPreferences"
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

    var subjectBadAverage by FloatPreference(
            key = GRADES_SUBJECT_BAD_AVERAGE,
            provider = accessProvider,
            defaultValue = 4.5F
    )

    private class Getter : PreferencesGetterAbs<GradesPreferences>() {

        override fun get(): GradesPreferences = instance

        override val dataClass: Class<out GradesPreferences>
            get() = GradesPreferences::class.java
    }
}

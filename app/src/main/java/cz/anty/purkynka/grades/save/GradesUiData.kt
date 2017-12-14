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
import cz.anty.purkynka.PrefNames.FILE_NAME_GRADES_UI_DATA
import cz.anty.purkynka.PrefNames.LAST_SORT_GRADES
import eu.codetopic.java.utils.JavaExtensions.kSerializer
import eu.codetopic.utils.data.preferences.VersionedPreferencesData
import eu.codetopic.utils.data.preferences.preference.EnumPreference
import eu.codetopic.utils.data.preferences.preference.KotlinSerializedPreference
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs

/**
 * @author anty
 */
class GradesUiData private constructor(context: Context) :
        VersionedPreferencesData<SharedPreferences>(context,
                BasicSharedPreferencesProvider(context, FILE_NAME_GRADES_UI_DATA), SAVE_VERSION) {

    companion object : PreferencesCompanionObject<GradesUiData>(GradesUiData.LOG_TAG, ::GradesUiData, ::Getter) {

        private const val LOG_TAG = "GradesUiData"
        private const val SAVE_VERSION = 0
    }

    override fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
        // This function will be executed by provider in provider process
        when (from) {
            -1 -> {
                // First start, nothing to do
            }
        } // No more versions yet
    }

    var lastSort by EnumPreference(
            LAST_SORT_GRADES,
            Sort::class,
            accessProvider,
            Sort.DATE
    )

    enum class Sort {
        DATE, SUBJECTS // TODO: more sort options
    }

    private class Getter : PreferencesGetterAbs<GradesUiData>() {

        override fun get(): GradesUiData = instance

        override val dataClass: Class<out GradesUiData>
            get() = GradesUiData::class.java
    }
}
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
import android.content.SharedPreferences
import cz.anty.purkynka.PrefNames
import eu.codetopic.utils.data.preferences.VersionedPreferencesData
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs

/**
 * @author anty
 */
class UpdateData private constructor(context: Context) :
        VersionedPreferencesData<SharedPreferences>(
                context,
                BasicSharedPreferencesProvider(context, PrefNames.FILE_NAME_UPDATE_DATA),
                SAVE_VERSION
        ) {

    companion object : PreferencesCompanionObject<UpdateData>(
            UpdateData.LOG_TAG,
            ::UpdateData,
            ::Getter
    ) {

        private const val LOG_TAG = "UpdateData"
        private const val SAVE_VERSION = 0
    }

    @Synchronized
    override fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
        when (from) {
            -1 -> {
                // First start, nothing to do
            } // No more versions yet
        }
    }

    // TODO: add latestVersionCode

    // TODO: add latestVersionName

    private class Getter : PreferencesGetterAbs<UpdateData>() {

        override fun get() = instance

        override val dataClass: Class<out UpdateData>
            get() = UpdateData::class.java
    }
}
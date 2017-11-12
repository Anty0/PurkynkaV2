/*
 * ApplicationPurkynka
 * Copyright (C)  2017  anty
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cz.anty.purkynka.settings

import android.content.Context
import android.content.SharedPreferences

import eu.codetopic.utils.NetworkManager.NetworkType
import eu.codetopic.utils.data.preferences.VersionedPreferencesData
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs

import cz.anty.purkynka.PrefNames.*
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject

/**
 * @author anty
 */
class SettingsData private constructor(context: Context) :
        VersionedPreferencesData<SharedPreferences>(context,
                BasicSharedPreferencesProvider(context, FILE_NAME_SETTINGS_DATA),
                SAVE_VERSION) {

    companion object : PreferencesCompanionObject<SettingsData>(SettingsData.LOG_TAG, ::SettingsData, ::Getter) {

        private const val LOG_TAG = "SettingsData"
        private const val SAVE_VERSION = 0
    }

    val requiredNetworkType: NetworkType
        @Synchronized get() =
            if (preferences.getBoolean(SettingsActivity.PREFERENCE_KEY_REFRESH_ON_WIFI, true))
                NetworkType.WIFI else NetworkType.ANY

    @Synchronized
    override fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
        when (from) {
            -1 -> {
                // First start, nothing to do
            } // No more versions yet
        }
    }

    private class Getter : PreferencesGetterAbs<SettingsData>() {

        override fun get(): SettingsData? {
            return instance
        }

        override fun getDataClass(): Class<SettingsData> {
            return SettingsData::class.java
        }
    }
}

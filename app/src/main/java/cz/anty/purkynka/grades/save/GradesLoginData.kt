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

package cz.anty.purkynka.grades.save

import android.content.Context
import android.content.SharedPreferences

import com.securepreferences.SecurePreferences

import eu.codetopic.utils.data.preferences.extension.LoginDataExtension
import eu.codetopic.utils.data.preferences.VersionedPreferencesData
import eu.codetopic.utils.data.preferences.provider.SecureSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs

import cz.anty.purkynka.PrefNames.*
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject

/**
 * @author anty
 */
class GradesLoginData private constructor(context: Context) :
        VersionedPreferencesData<SecurePreferences>(context,
                SecureSharedPreferencesProvider(context, FILE_NAME_GRADES_LOGIN_DATA, clearOnFail = true),
                SAVE_VERSION) {

    companion object : PreferencesCompanionObject<GradesLoginData>(GradesLoginData.LOG_TAG, ::GradesLoginData, GradesLoginData::Getter) {

        private val LOG_TAG = "GradesLoginData"
        private val SAVE_VERSION = 0
    }

    val loginData: LoginDataExtension<SecurePreferences> = LoginDataExtension(preferencesAccessor)

    @Synchronized override fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
        when (from) {
            -1 -> {
                // First start, nothing to do
            }
        } // No more versions yet
    }

    private class Getter : PreferencesGetterAbs<GradesLoginData>() {

        override fun get(): GradesLoginData? {
            return instance
        }

        override fun getDataClass(): Class<GradesLoginData> {
            return GradesLoginData::class.java
        }
    }
}

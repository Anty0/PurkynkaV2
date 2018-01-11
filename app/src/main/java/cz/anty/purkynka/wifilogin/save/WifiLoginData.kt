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

package cz.anty.purkynka.wifilogin.save

import android.content.Context
import android.content.SharedPreferences
import cz.anty.purkynka.PrefNames.FILE_NAME_WIFI_LOGIN_DATA
import eu.codetopic.utils.data.preferences.VersionedPreferencesData
import eu.codetopic.utils.data.preferences.extension.LoginDataExtension
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.provider.SecureSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs
import eu.codetopic.utils.data.preferences.support.SecurePreferences

/**
 * @author anty
 */
class WifiLoginData private constructor(context: Context) :
        VersionedPreferencesData<SecurePreferences<SharedPreferences>>(
                context,
                SecureSharedPreferencesProvider(
                        BasicSharedPreferencesProvider(context, FILE_NAME_WIFI_LOGIN_DATA)
                ),
                SAVE_VERSION
        ) {

    companion object : PreferencesCompanionObject<WifiLoginData>(
            WifiLoginData.LOG_TAG,
            ::WifiLoginData,
            ::Getter
    ) {

        private const val LOG_TAG = "WifiLoginData"
        private const val SAVE_VERSION = 0

        val loginData get() = instance.loginData
    }

    override fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
        when (from) {
            -1 -> {
                // First start, nothing to do
            }
        } // No more versions yet
    }

    val loginData: LoginDataExtension<SecurePreferences<SharedPreferences>>
            by lazy { LoginDataExtension(accessProvider) }

    private class Getter : PreferencesGetterAbs<WifiLoginData>() {

        override fun get() = instance

        override val dataClass: Class<out WifiLoginData>
            get() = WifiLoginData::class.java
    }
}
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
import cz.anty.purkynka.PrefNames.LOGIN_COUNTER
import eu.codetopic.java.utils.JavaExtensions
import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.preference.BooleanPreference
import eu.codetopic.utils.data.preferences.preference.EnumPreference
import eu.codetopic.utils.data.preferences.preference.IntPreference
import eu.codetopic.utils.data.preferences.preference.KotlinSerializedPreference
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.support.ContentProviderSharedPreferences
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.list
import kotlinx.serialization.map

/**
 * @author anty
 */
class WifiData private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, WifiDataProvider.AUTHORITY)) {

    companion object : PreferencesCompanionObject<WifiData>(
            WifiData.LOG_TAG,
            ::WifiData,
            ::Getter
    ) {

        private const val LOG_TAG = "WifiData"
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

    private val loginCounterPref = IntPreference(
            key = LOGIN_COUNTER,
            provider = accessProvider,
            defaultValue = 0
    )

    fun getLoginCount(accountId: String) = loginCounterPref.getValue(this, accountId)

    fun incrementLoginCounter(accountId: String) {
        loginCounterPref.getValue(this, accountId)
                .let { loginCounterPref.setValue(this, accountId, it) }
    }

    private class Getter : PreferencesGetterAbs<WifiData>() {

        override fun get(): WifiData = instance

        override val dataClass: Class<out WifiData>
            get() = WifiData::class.java
    }
}
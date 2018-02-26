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

package cz.anty.purkynka.update.save

import android.content.Context
import android.content.SharedPreferences
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.utils.*
import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.preference.IntPreference
import eu.codetopic.utils.data.preferences.preference.KSerializedPreference
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.support.ContentProviderSharedPreferences
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.PairSerializer
import kotlinx.serialization.internal.StringSerializer

/**
 * @author anty
 */
class UpdateData private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, UpdateDataProvider.AUTHORITY)) {

    companion object : PreferencesCompanionObject<UpdateData>(
            UpdateData.LOG_TAG,
            ::UpdateData,
            ::Getter
    ) {

        private const val LOG_TAG = "UpdateData"
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

    var lastKnownVersion by IntPreference(
            LAST_KNOWN_VERSION,
            accessProvider,
            -1
    )

    var jobScheduleVersion by IntPreference(
            SCHEDULE_VERSION,
            accessProvider,
            -1
    )

    var latestVersion by KSerializedPreference(
            key = LATEST_VERSION,
            serializer = PairSerializer(IntSerializer, StringSerializer),
            provider = accessProvider,
            defaultValue = BuildConfig.VERSION_CODE to BuildConfig.VERSION_NAME
    )

    private class Getter : PreferencesGetterAbs<UpdateData>() {

        override fun get() = instance

        override val dataClass: Class<out UpdateData>
            get() = UpdateData::class.java
    }
}
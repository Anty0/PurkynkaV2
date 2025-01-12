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

package cz.anty.purkynka.lunches.save

import android.content.Context
import android.content.SharedPreferences
import cz.anty.purkynka.utils.*
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import eu.codetopic.java.utils.kSerializer
import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.preference.BooleanPreference
import eu.codetopic.utils.data.preferences.preference.EnumPreference
import eu.codetopic.utils.data.preferences.preference.FloatPreference
import eu.codetopic.utils.data.preferences.preference.KSerializedPreference
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.support.ContentProviderSharedPreferences
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs
import kotlinx.serialization.list

/**
 * @author anty
 */
class LunchesData private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, LunchesDataProvider.AUTHORITY)) {

    companion object : PreferencesCompanionObject<LunchesData>(
            LunchesData.LOG_TAG,
            ::LunchesData,
            ::Getter
    ) {

        private const val LOG_TAG = "LunchesData"
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

    private val lastSyncResultPref = EnumPreference(
            SYNC_RESULT,
            SyncResult::class,
            accessProvider,
            SyncResult.SUCCESS
    )

    private val creditPref = FloatPreference(
            CREDIT,
            accessProvider,
            Float.NaN
    )

    private val invalidityPref = BooleanPreference(
            INVALID,
            accessProvider,
            false
    )

    fun setCredit(id: String, credit: Float) { creditPref[this, id] = credit }

    fun getCredit(id: String): Float = creditPref[this, id]

    fun makeDataValid(id: String) { invalidityPref[this, id] = false }

    fun invalidateData(id: String) { invalidityPref[this, id] = true }

    fun isDataValid(id: String): Boolean = !invalidityPref[this, id]

    fun getLastSyncResult(id: String): SyncResult = lastSyncResultPref[this, id]

    fun setLastSyncResult(id: String, value: SyncResult) { lastSyncResultPref[this, id] = value }

    private val lunchesPreference =
            KSerializedPreference<List<LunchOptionsGroup>>(
                    LUNCHES_LIST,
                    kSerializer<LunchOptionsGroup>().list,
                    accessProvider,
                    { emptyList() }
            )

    fun getLunches(id: String): List<LunchOptionsGroup> = lunchesPreference[this, id]

    fun setLunches(id: String, value: List<LunchOptionsGroup>) {
        lunchesPreference[this, id] = value
    }

    enum class SyncResult {
        SUCCESS, FAIL_LOGIN, FAIL_CONNECT, FAIL_UNKNOWN
    }

    private class Getter : PreferencesGetterAbs<LunchesData>() {

        override fun get(): LunchesData = instance

        override val dataClass: Class<out LunchesData>
            get() = LunchesData::class.java
    }
}

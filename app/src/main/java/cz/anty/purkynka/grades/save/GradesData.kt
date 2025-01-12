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
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.load.GradesParser.toSubjects
import cz.anty.purkynka.utils.GRADES_MAP
import cz.anty.purkynka.utils.SYNC_RESULT
import eu.codetopic.java.utils.kSerializer
import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.preference.EnumPreference
import eu.codetopic.utils.data.preferences.preference.KSerializedPreference
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
class GradesData private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, GradesDataProvider.AUTHORITY)) {

    companion object : PreferencesCompanionObject<GradesData>(
            GradesData.LOG_TAG,
            ::GradesData,
            ::Getter
    ) {

        private const val LOG_TAG = "GradesData"
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

    fun getLastSyncResult(id: String): SyncResult =
            lastSyncResultPref[this, id]

    fun setLastSyncResult(id: String, value: SyncResult) {
        lastSyncResultPref[this, id] = value
    }

    private val gradesPreference = KSerializedPreference<GradesMap>(
            GRADES_MAP,
            (IntSerializer to kSerializer<Grade>().list).map,
            accessProvider
    ) {
        mapOf(Semester.FIRST.value to emptyList(),
                Semester.SECOND.value to emptyList())
    }

    fun getGrades(id: String): GradesMap = gradesPreference[this, id]

    fun setGrades(id: String, value: GradesMap) {
        gradesPreference[this, id] = value
    }

    fun getSubjects(id: String): SubjectsMap {
        return synchronized(this) {
            getGrades(id).map {
                it.key to it.value.toSubjects()
            }.toMap()
        }
    }

    enum class SyncResult {
        SUCCESS, FAIL_LOGIN, FAIL_CONNECT, FAIL_UNKNOWN
    }

    private class Getter : PreferencesGetterAbs<GradesData>() {

        override fun get(): GradesData = instance

        override val dataClass: Class<out GradesData>
            get() = GradesData::class.java
    }
}

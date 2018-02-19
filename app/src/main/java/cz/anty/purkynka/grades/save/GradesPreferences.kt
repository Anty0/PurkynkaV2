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

    val subjectBadAverage by FloatPreference(
            key = GRADES_SUBJECT_BAD_AVERAGE,
            provider = accessProvider,
            defaultValue = 4.5F
    )

    /*private val dashboardDismissedSubjectsPref = KSerializedPreference<List<SubjectId>>(
            key = DASHBOARD_DISMISSED_SUBJECTS,
            serializer = kSerializer<SubjectId>().list,
            provider = accessProvider,
            defaultValue = { emptyList() }
    )

    fun dismissSubject(id: String, semester: Semester, subject: Subject) {
        val dismissed = dashboardDismissedSubjectsPref.getValue(this, id).toMutableList()
        dismissed.add(SubjectId(semester, subject))
        dashboardDismissedSubjectsPref.setValue(this, id, dismissed)
    }

    fun restoreSubject(id: String, semester: Semester, subject: Subject) {
        val dismissed = dashboardDismissedSubjectsPref.getValue(this, id).toMutableList()
        dismissed.remove(SubjectId(semester, subject))
        dashboardDismissedSubjectsPref.setValue(this, id, dismissed)
    }

    fun getDismissedSubjects(id: String): List<SubjectId> =
            dashboardDismissedSubjectsPref.getValue(this, id)

    @Serializable
    class SubjectId(val semesterValue: Int, val subjectShortName: String) {

        constructor(semester: Semester, subject: Subject)
                : this(semester.value, subject.shortName)

        fun idEquals(semester: Semester, subject: Subject): Boolean =
                this.semesterValue == semester.value
                        && this.subjectShortName == subject.shortName

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SubjectId

            if (semesterValue != other.semesterValue) return false
            if (subjectShortName != other.subjectShortName) return false

            return true
        }

        override fun hashCode(): Int {
            var result = semesterValue
            result = 31 * result + subjectShortName.hashCode()
            return result
        }
    }*/

    private class Getter : PreferencesGetterAbs<GradesPreferences>() {

        override fun get(): GradesPreferences = instance

        override val dataClass: Class<out GradesPreferences>
            get() = GradesPreferences::class.java
    }
}

/*
 * Copyright 2017 Jiří Kuchyňka (Anty)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package cz.anty.purkynka.grades.save

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cz.anty.purkynka.PrefNames.GRADES_MAP
import cz.anty.purkynka.grades.data.Subject
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.load.GradesParser.toSubjects

import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.extension.LoginDataExtension
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.provider.SecureSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.*

/**
 * @author anty
 */
class GradesData private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, GradesProvider.AUTHORITY)) {

    companion object : PreferencesCompanionObject<GradesData>(GradesData.LOG_TAG, ::GradesData, GradesData::Getter) {

        private const val LOG_TAG = "GradesData"
        internal const val SAVE_VERSION = 0

        internal fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
            // This function will be executed by provider in provider process
            when (from) {
                -1 -> {
                    // First start, nothing to do
                }
            } // No more versions yet
        }
    }

    private val gson: Gson = Gson()

    val loginData: LoginDataExtension<SecurePreferences<SharedPreferences>> by lazy {
        LoginDataExtension(SecureSharedPreferencesProvider<SharedPreferences>(accessProvider))
    }

    private val gradesPreference = GsonPreference<Map<Int, MutableList<Grade>>>(GRADES_MAP, gson,
            object: TypeToken<Map<Int, MutableList<Grade>>>(){}.type, accessProvider) {
        mapOf(Semester.FIRST.value to mutableListOf(),
                Semester.SECOND.value to mutableListOf())
    }

    var grades by gradesPreference

    fun getGrades(id: String?): Map<Int, MutableList<Grade>> {
        return gradesPreference.getValue(this, id)
    }

    fun setGrades(id: String?, value: Map<Int, MutableList<Grade>>) {
        gradesPreference.setValue(this, id, value)
    }

    val lessons: Map<Int, List<Subject>>
        get() {
            return getLessons(null)
        }

    fun getLessons(id: String?): Map<Int, List<Subject>> {
        return synchronized(this) {
            getGrades(id).map {
                it.key to it.value.toSubjects()
            }.toMap()
        }
    }

    private class Getter : PreferencesGetterAbs<GradesData>() {

        override fun get(): GradesData? {
            return instance
        }

        override fun getDataClass(): Class<GradesData> {
            return GradesData::class.java
        }
    }
}

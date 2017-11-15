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

package cz.anty.purkynka.marks

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cz.anty.purkynka.PrefNames.MARKS_MAP
import cz.anty.purkynka.marks.data.Lesson
import cz.anty.purkynka.marks.data.Mark
import cz.anty.purkynka.marks.data.Semester

import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.support.ContentProviderSharedPreferences
import eu.codetopic.utils.data.preferences.support.GsonPreference
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs

import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject

/**
 * @author anty
 */
class MarksData private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, MarksProvider.AUTHORITY)) {

    companion object : PreferencesCompanionObject<MarksData>(MarksData.LOG_TAG, ::MarksData, ::Getter) {

        private val LOG_TAG = "MarksData"
        internal val SAVE_VERSION = 0

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

    var marks by GsonPreference<Map<Int, MutableList<Mark>>>(MARKS_MAP, gson,
            object: TypeToken<Map<Int, MutableList<Mark>>>(){}.type, preferencesAccessor) {
        mapOf(Semester.FIRST.value to mutableListOf(),
            Semester.SECOND.value to mutableListOf())
    }

    val lessons: Map<Int, MutableList<Lesson>>
        get() {
            synchronized(this) {
                return marks.map {
                    it.key to it.value.let {
                        mutableMapOf<String, MutableList<Mark>>().apply {
                            it.forEach {
                                getOrPut(it.shortLesson, ::mutableListOf).add(it)
                            }
                        }.map {
                            Lesson(it.value.first().longLesson,
                                    it.value.first().shortLesson,
                                    it.value.toList())
                        }.toMutableList()
                    }
                }.toMap()
            }
        }

    private class Getter : PreferencesGetterAbs<MarksData>() {

        override fun get(): MarksData? {
            return instance
        }

        override fun getDataClass(): Class<MarksData> {
            return MarksData::class.java
        }
    }
}

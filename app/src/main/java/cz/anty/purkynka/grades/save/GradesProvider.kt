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
import cz.anty.purkynka.PrefNames.FILE_NAME_GRADES_DATA
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.provider.ISharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.VersionedContentProviderPreferences

/**
 * @author anty
 */
class GradesProvider : VersionedContentProviderPreferences<SharedPreferences>(AUTHORITY, GradesData.SAVE_VERSION) {

    companion object {
        const val AUTHORITY = "cz.anty.purkynka.grades.data"
    }

    override fun onPreparePreferencesProvider(): ISharedPreferencesProvider<SharedPreferences> {
        return BasicSharedPreferencesProvider(context, FILE_NAME_GRADES_DATA, Context.MODE_PRIVATE)
    }

    override fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
        GradesData.onUpgrade(editor, from, to)
    }
}
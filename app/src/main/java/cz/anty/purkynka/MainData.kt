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

package cz.anty.purkynka

import android.content.Context
import android.content.SharedPreferences

import eu.codetopic.utils.data.preferences.VersionedPreferencesData
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs

import cz.anty.purkynka.PrefNames.*
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject

/**
 * @author anty
 */
class MainData private constructor(context: Context) :
        VersionedPreferencesData<SharedPreferences>(context,
                BasicSharedPreferencesProvider(context, FILE_NAME_MAIN_DATA),
                SAVE_VERSION) {

    companion object : PreferencesCompanionObject<MainData>(MainData.LOG_TAG, ::MainData, ::Getter) {

        private const val LOG_TAG = "MainData"
        private const val SAVE_VERSION = 0
    }

    @Synchronized
    override fun onUpgrade(editor: SharedPreferences.Editor, from: Int, to: Int) {
        when (from) {
            -1 -> {
                // First start, nothing to do
            } // No more versions yet
        }
    }

    private class Getter : PreferencesGetterAbs<MainData>() {

        override fun get(): MainData {
            return instance
        }

        override fun getDataClass(): Class<MainData> {
            return MainData::class.java
        }
    }
}

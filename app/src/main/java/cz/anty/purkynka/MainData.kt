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

package cz.anty.purkynka

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

import eu.codetopic.utils.data.getter.DataGetter
import eu.codetopic.utils.data.preferences.VersionedPreferencesData
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs

import cz.anty.purkynka.PrefNames.*

/**
 * @author anty
 */
class MainData private constructor(context: Context) :
        VersionedPreferencesData<SharedPreferences>(context,
                BasicSharedPreferencesProvider(context, FILE_NAME_MAIN_DATA, Context.MODE_PRIVATE),
                SAVE_VERSION) {

    companion object {

        val getter: DataGetter<MainData> = Getter()

        private const val LOG_TAG = "MainData"
        private const val SAVE_VERSION = 0

        @SuppressLint("StaticFieldLeak")
        private var instance: MainData? = null

        @Synchronized
        fun initialize(context: Context) {
            if (instance != null) throw IllegalStateException("$LOG_TAG is still initialized")
            instance = MainData(context)
            instance?.init()
        }
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
            return instance ?: throw IllegalStateException("$LOG_TAG is not initialized")
        }

        override fun getDataClass(): Class<MainData> {
            return MainData::class.java
        }
    }
}

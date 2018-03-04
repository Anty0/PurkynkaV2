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

package cz.anty.purkynka.feedback.save

import android.content.Context
import android.content.SharedPreferences
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.utils.LAST_ERROR_VERSION
import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.preference.IntPreference
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.support.ContentProviderSharedPreferences
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs

/**
 * @author anty
 */
class FeedbackData private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, FeedbackDataProvider.AUTHORITY)) {

    companion object : PreferencesCompanionObject<FeedbackData>(
            FeedbackData.LOG_TAG,
            ::FeedbackData,
            ::Getter
    ) {

        private const val LOG_TAG = "FeedbackData"
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

    private var lastErrorVersion by IntPreference(
            key = LAST_ERROR_VERSION,
            provider = accessProvider,
            defaultValue = -1
    )

    val isFeedbackEnabled: Boolean
        get() = lastErrorVersion == BuildConfig.VERSION_CODE

    fun notifyErrorReceived() {
        lastErrorVersion = BuildConfig.VERSION_CODE
    }

    fun notifyFeedbackDone() {
        lastErrorVersion = -1
    }

    private class Getter : PreferencesGetterAbs<FeedbackData>() {

        override fun get(): FeedbackData = FeedbackData.instance

        override val dataClass: Class<out FeedbackData>
            get() = FeedbackData::class.java
    }
}
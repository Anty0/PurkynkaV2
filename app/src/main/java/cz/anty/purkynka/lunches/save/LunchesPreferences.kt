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
import cz.anty.purkynka.utils.SHOW_DASHBOARD_CREDIT_WARNING
import cz.anty.purkynka.utils.WIDGET_ACCOUNT_ID
import eu.codetopic.utils.data.preferences.PreferencesData
import eu.codetopic.utils.data.preferences.preference.BooleanPreference
import eu.codetopic.utils.data.preferences.preference.StringPreference
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider
import eu.codetopic.utils.data.preferences.support.ContentProviderSharedPreferences
import eu.codetopic.utils.data.preferences.support.PreferencesCompanionObject
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs

/**
 * @author anty
 */
class LunchesPreferences private constructor(context: Context) :
        PreferencesData<ContentProviderSharedPreferences>(context,
                ContentProviderPreferencesProvider(context, LunchesPreferencesProvider.AUTHORITY)) {

    companion object : PreferencesCompanionObject<LunchesPreferences>(
            LunchesPreferences.LOG_TAG,
            ::LunchesPreferences,
            ::Getter
    ) {

        private const val LOG_TAG = "LunchesPreferences"
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

    var showDashboardCreditWarning by BooleanPreference(
            key = SHOW_DASHBOARD_CREDIT_WARNING,
            provider = accessProvider,
            defaultValue = true
    )

    private val appWidgetAccountIdPref = StringPreference(
            key = WIDGET_ACCOUNT_ID,
            provider = accessProvider,
            defaultValue = "" // empty string == not set
    )

    fun getAppWidgetAccountId(appWidgetId: Int): String? =
            appWidgetAccountIdPref[this, appWidgetId.toString()]
                    .takeIf { it.isNotEmpty() }

    fun setAppWidgetAccountId(appWidgetId: Int, accountId: String) {
        appWidgetAccountIdPref[this, appWidgetId.toString()] = accountId
    }

    fun removeAppWidgetAccountId(appWidgetId: Int) {
        appWidgetAccountIdPref.unset(this, appWidgetId.toString())
    }

    private class Getter : PreferencesGetterAbs<LunchesPreferences>() {

        override fun get(): LunchesPreferences = instance

        override val dataClass: Class<out LunchesPreferences>
            get() = LunchesPreferences::class.java
    }
}
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

package cz.anty.purkynka.settings

import android.content.Context
import android.os.Bundle
import android.support.v14.preference.PreferenceFragment
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceDataStore
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import cz.anty.purkynka.R
import cz.anty.purkynka.grades.save.GradesPreferences
import cz.anty.purkynka.lunches.notify.LunchesChangesNotifyChannel
import cz.anty.purkynka.lunches.save.LunchesPreferences
import cz.anty.purkynka.update.ui.UpdateActivity
import cz.anty.purkynka.utils.URL_FACEBOOK_PAGE
import cz.anty.purkynka.utils.URL_WEB_PAGE
import eu.codetopic.java.utils.format
import eu.codetopic.utils.AndroidUtils
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import org.jetbrains.anko.ctx

/**
 * @author anty
 */

class SettingsActivity : ModularActivity(BackButtonModule()) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager
                .beginTransaction()
                .replace(android.R.id.content, CustomPreferenceFragment())
                .commit()
    }
}

private val sBindPreferenceSummaryToValueListener =
        Preference.OnPreferenceChangeListener listener@ { preference, value ->
            val stringValue = value.toString()
            if (preference is ListPreference) {
                val index = preference.findIndexOfValue(stringValue)
                preference.setSummary(
                        if (index >= 0) preference.entries[index]
                        else null
                )
            } else {
                preference.summary = stringValue
            }
            return@listener true
        }

private fun bindPreferenceSummaryToValue(preference: Preference) {
    preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
    sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
            preference.sharedPreferences?.getString(preference.key, "")
                    ?: preference.preferenceDataStore?.getString(preference.key, ""))
}

class CustomPreferenceFragment : PreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = CustomDataStorage(ctx)

        addPreferencesFromResource(R.xml.settings)

        bindPreferenceSummaryToValue(findPreference(
                getString(R.string.pref_key_grades_bad_average)))

        findPreference(getString(R.string.pref_key_activity_update)).setOnPreferenceClickListener listener@ {
            UpdateActivity.start(activity)
            return@listener true
        }
        findPreference(getString(R.string.pref_key_activity_about)).setOnPreferenceClickListener listener@ {
            LibsBuilder()
                    .withActivityTheme(R.style.AppTheme_NoActionBar)
                    .withActivityStyle(Libs.ActivityStyle.DARK)
                    .withActivityTitle(getString(R.string.pref_about))
                    .withAboutIconShown(true)
                    .withAboutAppName(getString(R.string.app_name))
                    .withAboutVersionShownName(true)
                    .withAboutDescription(
                            "Copyright (C)   2018  Anty<br />" +
                                    "<br />" +
                                    "This program is free software: you can redistribute it and/or modify" +
                                    " it under the terms of the GNU General Public License as published by" +
                                    " the Free Software Foundation, either version 3 of the License, or" +
                                    " (at your option) any later version.<br />" +
                                    "<br />" +
                                    "This program is distributed in the hope that it will be useful," +
                                    " but WITHOUT ANY WARRANTY; without even the implied warranty of" +
                                    " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the" +
                                    " GNU General Public License for more details.<br />" +
                                    "<br />" +
                                    "You should have received a copy of the GNU General Public License" +
                                    " along with this program. If not, see <http://www.gnu.org/licenses/>."
                    )
                    .withLicenseShown(true)
                    .withAutoDetect(true)
                    .withFields(R.string::class.java.fields)
                    .start(ctx)
            return@listener true
        }
        findPreference(getString(R.string.pref_key_activity_facebook)).setOnPreferenceClickListener listener@ {
            AndroidUtils.openUri(ctx, URL_FACEBOOK_PAGE, R.string.toast_browser_failed)
            return@listener true
        }
        findPreference(getString(R.string.pref_key_activity_web_page)).setOnPreferenceClickListener listener@ {
            AndroidUtils.openUri(ctx, URL_WEB_PAGE, R.string.toast_browser_failed)
            return@listener true
        }
        /*findPreference(getString(R.string.pref_key_activity_web_page_donate)).setOnPreferenceClickListener listener@ {
            AndroidUtils.openUri(ctx, URL_WEB_DONATE_PAGE, R.string.toast_browser_failed)
            return@listener true
        }*/
    }
}

class CustomDataStorage(private val context: Context) : PreferenceDataStore() {

    companion object {

        private const val LOG_TAG = "CustomDataStorage"
    }

    private val prefsMap: Map<String, Int> = arrayOf(
            R.string.pref_key_grades_bad_average,
            R.string.pref_key_lunches_show_dashboard_credit_warning,
            R.string.pref_key_lunches_notify_lunches_new
    ).map { context.getString(it) to it }.toMap()

    override fun getBoolean(key: String, defValue: Boolean): Boolean = when (prefsMap[key]) {
        R.string.pref_key_lunches_show_dashboard_credit_warning ->
            LunchesPreferences.instance.showDashboardCreditWarning
        R.string.pref_key_lunches_notify_lunches_new ->
            NotifyManager.isChannelEnabled(
                    groupId = null,
                    channelId = LunchesChangesNotifyChannel.ID
            )
        else -> super.getBoolean(key, defValue)
    }

    override fun putBoolean(key: String, value: Boolean) = when (prefsMap[key]) {
        R.string.pref_key_lunches_show_dashboard_credit_warning ->
            LunchesPreferences.instance.showDashboardCreditWarning = value
        R.string.pref_key_lunches_notify_lunches_new ->
                NotifyManager.requestSetChannelEnabled(
                        context = context,
                        groupId = null,
                        channelId = LunchesChangesNotifyChannel.ID,
                        enable = value
                )
        else -> super.putBoolean(key, value)
    }

    override fun getString(key: String, defValue: String?): String? = when (prefsMap[key]) {
        R.string.pref_key_grades_bad_average ->
            GradesPreferences.instance.badAverage.format(2)
        else -> super.getString(key, defValue)
    }

    override fun putString(key: String, value: String?) = when (prefsMap[key]) {
        R.string.pref_key_grades_bad_average ->
            GradesPreferences.instance.badAverage = value?.toFloatOrNull()
                    ?: throw IllegalArgumentException("Invalid value: $value")
        else -> super.putString(key, value)
    }
}
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

package cz.anty.purkynka.settings;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.util.Colors;

import cz.anty.purkynka.Constants;
import cz.anty.purkynka.R;
import cz.anty.purkynka.update.UpdateActivity;
import eu.codetopic.utils.AndroidUtils;
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule;
import eu.codetopic.utils.ui.activity.modular.ModularActivity;

/**
 * Created by anty on 6/21/17.
 *
 * @author anty
 */
public class SettingsActivity extends ModularActivity {

    static final String PREFERENCE_KEY_REFRESH_ON_WIFI = "REFRESH_ON_WIFI";

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value.toString();
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            preference.setSummary(index >= 0
                    ? listPreference.getEntries()[index] : null);
        } else {
            preference.setSummary(stringValue);
        }
        return true;
    };

    public SettingsActivity() {
        super(new BackButtonModule());
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                preference.getSharedPreferences().getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new CustomPreferenceFragment()).commit();
    }

    public static class CustomPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            findPreference(PREFERENCE_KEY_REFRESH_ON_WIFI).setOnPreferenceChangeListener((preference, o) -> {
                /*TimedComponentsManager.getInstance().setRequiredNetwork(
                        SettingsData.Companion.getInstance().getRequiredNetworkType());*/ // TODO: apply change to runtime
                return true;
            });
            findPreference("activity_update").setOnPreferenceClickListener(preference -> {
                UpdateActivity.Companion.start(getActivity());
                return true;
            });
            findPreference("activity_about").setOnPreferenceClickListener(preference -> {
                new LibsBuilder()
                        //.withActivityTheme(R.style.AppTheme_NoActionBar)
                        .withActivityStyle(Libs.ActivityStyle.DARK)
                        //.withActivityColor(new Colors(
                        //        ContextCompat.getColor(getActivity(), R.color.colorPrimary),
                        //        ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark)))
                        .withActivityTitle(getString(R.string.pref_about))
                        .withAboutIconShown(true)
                        .withAboutAppName(getString(R.string.app_name))
                        .withAboutVersionShownName(true)
                        .withAboutDescription("Copyright (c) 2017 Codetopic, All Rights Reserved. " // TODO: to strings and translate
                                + " THIS SOFTWARE IS PROVIDED \"AS IS\" WITHOUT WARRANTY " // TODO: add (use) GPLv3 license head
                                + "OF ANY KIND. IN NO EVENT SHALL THE COPYRIGHT HOLDERS "
                                + "OR AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR "
                                + "OTHER LIABILITY, ARISING FROM USE OF THIS SOFTWARE OR IN "
                                + "CONNECTION WITH THIS SOFTWARE.")
                        .withLicenseShown(true)
                        .withAutoDetect(true)
                        .withFields(R.string.class.getFields())
                        .start(getActivity());
                return true;
            });
            findPreference("activity_facebook").setOnPreferenceClickListener(preference -> {
                AndroidUtils.openUri(getActivity(), Constants.URL_FACEBOOK_PAGE, R.string.toast_browser_failed);
                return true;
            });
            findPreference("activity_web_page").setOnPreferenceClickListener(preference -> {
                AndroidUtils.openUri(getActivity(), Constants.URL_WEB_PAGE, R.string.toast_browser_failed);
                return true;
            });
            findPreference("activity_web_page_donate").setOnPreferenceClickListener(preference -> {
                AndroidUtils.openUri(getActivity(), Constants.URL_WEB_DONATE_PAGE, R.string.toast_browser_failed);
                return true;
            });
        }
    }
}
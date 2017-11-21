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

package cz.anty.purkynka.settings;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.util.Colors;

import cz.anty.purkynka.Constants;
import cz.anty.purkynka.R;
import eu.codetopic.utils.AndroidUtils;
import eu.codetopic.utils.timing.TimedComponentsManager;
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
                TimedComponentsManager.getInstance().setRequiredNetwork(
                        SettingsData.Companion.getInstance().getRequiredNetworkType());
                return true;
            });
            findPreference("activity_about").setOnPreferenceClickListener(preference -> {
                new LibsBuilder()
                        .withActivityStyle(Libs.ActivityStyle.DARK)
                        .withActivityColor(new Colors(
                                ContextCompat.getColor(getActivity(), R.color.colorPrimary),
                                ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark)))
                        .withActivityTitle(getString(R.string.pref_header_about))
                        .withAboutIconShown(true)
                        .withAboutAppName(getString(R.string.app_name))
                        .withAboutVersionShownName(true)
                        .withAboutDescription("Copyright (c) 2017 Codetopic, All Rights Reserved. " // TODO: to strings and translate
                                + " THIS SOFTWARE IS PROVIDED \"AS IS\" WITHOUT WARRANTY " // TODO: add GPLv3 license head
                                + "OF ANY KIND. IN NO EVENT SHALL THE COPYRIGHT HOLDERS "
                                + "OR AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR "
                                + "OTHER LIABILITY, ARISING FROM USE OF THIS SOFTWARE OR IN "
                                + "CONNECTION WITH THIS SOFTWARE.")
                        .withLicenseShown(true)
                        .withAutoDetect(false)
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
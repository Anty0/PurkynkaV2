package cz.anty.purkynka.settings;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.util.Colors;

import cz.anty.purkynka.Constants;
import cz.anty.purkynka.R;
import eu.codetopic.utils.timing.TimedComponentsManager;
import eu.codetopic.utils.ui.activity.BackButtonModule;
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
                        SettingsData.getter.get().getRequiredNetworkType());
                return true;
            });
            findPreference("activity_about").setOnPreferenceClickListener(preference -> {
                new LibsBuilder()
                        .withActivityStyle(Libs.ActivityStyle.DARK)
                        .withActivityColor(new Colors(Color.parseColor("#212121"),
                                Color.parseColor("#000000")))
                        .withActivityTitle(getString(R.string.pref_header_about))
                        .withAboutIconShown(true)
                        .withAboutAppName(getString(R.string.app_name))
                        .withAboutVersionShownName(true)
                        .withAboutDescription("Copyright (c) 2017 Codetopic, All Rights Reserved. " // TODO: to strings and translate
                                + " THIS SOFTWARE IS PROVIDED \"AS IS\" WITHOUT WARRANTY "
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
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_FACEBOOK_PAGE)));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.toast_browser_failed,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            findPreference("activity_web_page").setOnPreferenceClickListener(preference -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_WEB_PAGE)));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.toast_browser_failed,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            findPreference("activity_web_page_donate").setOnPreferenceClickListener(preference -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_WEB_PAGE_DONATE)));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.toast_browser_failed,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }
}
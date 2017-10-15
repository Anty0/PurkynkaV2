package cz.anty.purkynka.settings;

import android.content.Context;

import eu.codetopic.utils.NetworkManager;
import eu.codetopic.utils.NetworkManager.NetworkType;
import eu.codetopic.utils.data.getter.DataGetter;
import eu.codetopic.utils.data.preferences.PreferencesGetterAbs;
import eu.codetopic.utils.data.preferences.SharedPreferencesData;

import static cz.anty.purkynka.PrefNames.*;

/**
 * Created by anty on 6/21/17.
 *
 * @author anty
 */
public class SettingsData extends SharedPreferencesData {

    public static final DataGetter<SettingsData> getter = new Getter();

    private static final String LOG_TAG = "SettingsData";
    private static final int SAVE_VERSION = 0;

    private static SettingsData mInstance = null;

    private SettingsData(Context context) {
        super(context, FILE_NAME_SETTINGS_DATA, SAVE_VERSION);
    }

    public static void initialize(Context context) {
        if (mInstance != null) throw new IllegalStateException(LOG_TAG + " is still initialized");
        mInstance = new SettingsData(context);
        mInstance.init();
    }

    public NetworkType getRequiredNetworkType() {
        return getPreferences().getBoolean(SettingsActivity.PREFERENCE_KEY_REFRESH_ON_WIFI, true)
                ? NetworkType.WIFI : NetworkType.ANY;
    }

    private static final class Getter extends PreferencesGetterAbs<SettingsData> {

        @Override
        public SettingsData get() {
            return mInstance;
        }

        @Override
        public Class<SettingsData> getDataClass() {
            return SettingsData.class;
        }
    }
}

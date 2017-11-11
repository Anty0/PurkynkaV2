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

package cz.anty.purkynka.settings;

import android.content.Context;
import android.content.SharedPreferences;

import eu.codetopic.utils.NetworkManager.NetworkType;
import eu.codetopic.utils.data.getter.DataGetter;
import eu.codetopic.utils.data.preferences.VersionedPreferencesData;
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider;
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs;

import static cz.anty.purkynka.PrefNames.*;

/**
 * @author anty
 */
public final class SettingsData extends VersionedPreferencesData<SharedPreferences> {

    public static final DataGetter<SettingsData> getter = new Getter();

    private static final String LOG_TAG = "SettingsData";
    private static final int SAVE_VERSION = 0;

    private static SettingsData mInstance = null;

    private SettingsData(Context context) {
        super(context, new BasicSharedPreferencesProvider(context,
                FILE_NAME_SETTINGS_DATA, Context.MODE_PRIVATE), SAVE_VERSION);
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

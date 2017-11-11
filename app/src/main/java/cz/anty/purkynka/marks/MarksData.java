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

package cz.anty.purkynka.marks;

import android.content.Context;
import android.content.SharedPreferences;

import eu.codetopic.utils.data.getter.DataGetter;
import eu.codetopic.utils.data.preferences.PreferencesData;
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider;
import eu.codetopic.utils.data.preferences.provider.ContentProviderPreferencesProvider;
import eu.codetopic.utils.data.preferences.support.ContentProviderSharedPreferences;
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs;

import static cz.anty.purkynka.PrefNames.*;

/**
 * @author anty
 */
public final class MarksData extends PreferencesData<ContentProviderSharedPreferences> {

    public static final DataGetter<MarksData> getter = new Getter();

    private static final String LOG_TAG = "MarksData";
    public static final int SAVE_VERSION = 0;

    private static MarksData mInstance = null;

    private MarksData(Context context) {
        super(context, new ContentProviderPreferencesProvider(context, MarksProvider.AUTHORITY));
    }

    public static void initialize(Context context) {
        if (mInstance != null) throw new IllegalStateException(LOG_TAG + " is still initialized");
        mInstance = new MarksData(context);
        mInstance.init();
    }

    static void onUpgrade(SharedPreferences.Editor editor, int from, int to) {
        // This function will be executed by provider in provider process
        switch (from) {
            case -1:
                break; // First start, nothing to do
            // No more versions yet
        }
    }

    // TODO: get + set marks

    private static final class Getter extends PreferencesGetterAbs<MarksData> {

        @Override
        public MarksData get() {
            return mInstance;
        }

        @Override
        public Class<MarksData> getDataClass() {
            return MarksData.class;
        }
    }
}

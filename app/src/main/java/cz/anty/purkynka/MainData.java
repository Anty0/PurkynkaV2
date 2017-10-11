package cz.anty.purkynka;

import android.content.Context;

import eu.codetopic.utils.data.getter.DataGetter;
import eu.codetopic.utils.data.preferences.PreferencesGetterAbs;
import eu.codetopic.utils.data.preferences.SharedPreferencesData;

import static cz.anty.purkynka.PrefNames.*;

/**
 * Created by anty on 6/20/17.
 *
 * @author anty
 */
public class MainData extends SharedPreferencesData {

    public static final DataGetter<MainData> getter = new Getter();

    private static final String LOG_TAG = "MainData";
    private static final int SAVE_VERSION = 0;

    private static MainData mInstance = null;

    private MainData(Context context) {
        super(context, PREF_FILE_NAME_MAIN_DATA, SAVE_VERSION);
    }

    public static void initialize(Context context) {
        if (mInstance != null) throw new IllegalStateException(LOG_TAG + " is still initialized");
        mInstance = new MainData(context);
        mInstance.init();
    }

    private static final class Getter extends PreferencesGetterAbs<MainData> {

        @Override
        public MainData get() {
            return mInstance;
        }

        @Override
        public Class<MainData> getDataClass() {
            return MainData.class;
        }
    }
}

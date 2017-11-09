package cz.anty.purkynka.marks;

import android.content.Context;
import android.content.SharedPreferences;

import com.securepreferences.SecurePreferences;

import eu.codetopic.utils.data.getter.DataGetter;
import eu.codetopic.utils.data.preferences.LoginData;
import eu.codetopic.utils.data.preferences.provider.SecureSharedPreferencesProvider;
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs;

import static cz.anty.purkynka.PrefNames.*;

/**
 * Created by anty on 6/20/17.
 *
 * @author anty
 */
public class MarksLoginData extends LoginData<SecurePreferences> {

    public static final DataGetter<MarksLoginData> getter = new Getter();

    private static final String LOG_TAG = "MarksLoginData";
    private static final int SAVE_VERSION = 0;

    private static MarksLoginData mInstance = null;

    private MarksLoginData(Context context) {
        super(context, new SecureSharedPreferencesProvider(context, FILE_NAME_MARKS_LOGIN_DATA,
                SecureSharedPreferencesProvider.DEFAULT_PASSWORD, true), SAVE_VERSION);
    }

    public static void initialize(Context context) {
        if (mInstance != null) throw new IllegalStateException(LOG_TAG + " is still initialized");
        mInstance = new MarksLoginData(context);
        mInstance.init();
    }

    private static final class Getter extends PreferencesGetterAbs<MarksLoginData> {

        @Override
        public MarksLoginData get() {
            return mInstance;
        }

        @Override
        public Class<MarksLoginData> getDataClass() {
            return MarksLoginData.class;
        }
    }
}

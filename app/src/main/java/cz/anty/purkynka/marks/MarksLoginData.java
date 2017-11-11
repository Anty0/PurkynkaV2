package cz.anty.purkynka.marks;

import android.content.Context;
import android.content.SharedPreferences;

import com.securepreferences.SecurePreferences;

import org.jetbrains.annotations.NotNull;

import eu.codetopic.utils.data.getter.DataGetter;
import eu.codetopic.utils.data.preferences.extension.LoginDataExtension;
import eu.codetopic.utils.data.preferences.VersionedPreferencesData;
import eu.codetopic.utils.data.preferences.provider.SecureSharedPreferencesProvider;
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs;

import static cz.anty.purkynka.PrefNames.*;

/**
 * @author anty
 */
public final class MarksLoginData extends VersionedPreferencesData<SecurePreferences> {

    public static final DataGetter<MarksLoginData> getter = new Getter();

    private static final String LOG_TAG = "MarksLoginData";
    private static final int SAVE_VERSION = 0;

    private static MarksLoginData mInstance = null;

    private final LoginDataExtension<SecurePreferences> loginData;

    private MarksLoginData(Context context) {
        super(context, new SecureSharedPreferencesProvider(context, FILE_NAME_MARKS_LOGIN_DATA,
                SecureSharedPreferencesProvider.DEFAULT_PASSWORD, true), SAVE_VERSION);
        loginData = new LoginDataExtension<>(getPreferencesAccessor());
    }

    public static void initialize(Context context) {
        if (mInstance != null) throw new IllegalStateException(LOG_TAG + " is still initialized");
        mInstance = new MarksLoginData(context);
        mInstance.init();
    }

    public LoginDataExtension<SecurePreferences> getLoginData() {
        return loginData;
    }

    @Override
    protected synchronized void onUpgrade(@NotNull SharedPreferences.Editor editor, int from, int to) {
        switch (from) {
            case -1:
                break; // First start, nothing to do
            // No more versions yet
        }
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

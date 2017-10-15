package cz.anty.purkynka.marks;

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
public class MarksData extends SharedPreferencesData {

    public static final DataGetter<MarksData> getter = new Getter();

    private static final String LOG_TAG = "MarksData";
    private static final int SAVE_VERSION = 0;

    private static MarksData mInstance = null;

    private MarksData(Context context) {
        super(context, FILE_NAME_MARKS_DATA, SAVE_VERSION);
    }

    public static void initialize(Context context) {
        if (mInstance != null) throw new IllegalStateException(LOG_TAG + " is still initialized");
        mInstance = new MarksData(context);
        mInstance.init();
    }

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

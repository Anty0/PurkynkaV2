package cz.anty.purkynka.marks;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.Calendar;

import cz.anty.purkynka.Constants;
import eu.codetopic.java.utils.log.Log;
import eu.codetopic.utils.timing.info.TimedComponent;

/**
 * Created by anty on 6/20/17.
 *
 * @author anty
 */
public class MarksSyncService extends Service {

    private static final String LOG_TAG = "MarksSyncService";

    private static final Object sSyncAdapterLock = new Object();
    private static MarksSyncAdapter sSyncAdapter = null;

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new MarksSyncAdapter(getApplicationContext());
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}

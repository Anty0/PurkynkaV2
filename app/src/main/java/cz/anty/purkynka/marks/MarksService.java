package cz.anty.purkynka.marks;

import android.app.IntentService;
import android.content.Intent;
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
@TimedComponent(
        repeatTime = Constants.REPEAT_TIME_MARKS_UPDATE,
        usableDays = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY},
        startHour = 7,
        stopHour = 17,
        requiresInternetAccess = true,
        wakeUpForExecute = true
)
public class MarksService extends IntentService {

    private static final String LOG_TAG = "MarksService";

    public MarksService() {
        super(LOG_TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(LOG_TAG, "onHandleIntent was called");
    }
}

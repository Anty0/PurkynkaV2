package cz.anty.purkynka.marks.data;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by anty on 6/20/17.
 *
 * @author anty
 */
public enum Semester {
    FIRST, SECOND, AUTO;

    public Semester getStableSemester() {
        switch (getValue()) {
            case 2:
                return SECOND;
            case 1:
            default:
                return FIRST;
        }
    }

    public Integer getValue() {
        switch (this) {
            case FIRST:
                return 1;
            case SECOND:
                return 2;
            case AUTO:
            default:
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date(System.currentTimeMillis()));
                int month = calendar.get(Calendar.MONTH);
                return month > Calendar.JANUARY && month < Calendar.JULY ? 2 : 1;
        }
    }

    public Semester reverse() {
        switch (getValue()) {
            case 2:
                return FIRST;
            case 1:
            default:
                return SECOND;
        }
    }
}

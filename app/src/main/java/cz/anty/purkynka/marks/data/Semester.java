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

/*
 * app
 * Copyright (C)   2017  anty
 *
 * This program is free  software: you can redistribute it and/or modify
 * it under the terms  of the GNU General Public License as published by
 * the Free Software  Foundation, either version 3 of the License, or
 * (at your option) any  later version.
 *
 * This program is distributed in the hope that it  will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied  warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.   See the
 * GNU General Public License for more details.
 *
 * You  should have received a copy of the GNU General Public License
 * along  with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cz.anty.purkynka;

/**
 * @author anty
 */
public final class PrefNames {

    private PrefNames() {}

    // Other
    /// MainData
    public static final String FILE_NAME_MAIN_DATA = "MainData";
    /// ActiveAccountManager
    public static final String FILE_NAME_ACTIVE_ACCOUNT_DATA = "ActiveAccountData";
    public static final String ACTIVE_ACCOUNT_NAME = "ACTIVE_ACCOUNT_NAME";
    /// SettingsData
    public static final String FILE_NAME_SETTINGS_DATA = "SettingsData";
    // Grades
    /// GradesUiData
    public static final String FILE_NAME_GRADES_UI_DATA = "GradesUI";
    public static final String LAST_SORT_GRADES = "SORT_GRADES";
    /// GradesLoginData
    public static final String FILE_NAME_GRADES_LOGIN_DATA = "GradesLoginData";
    /// GradesData
    public static final String FILE_NAME_GRADES_DATA = "GradesData";
    public static final String FIRST_SYNC = "FIRST_SYNC";
    public static final String SYNC_RESULT = "SYNC_RESULT";
    public static final String GRADES_MAP = "GRADES";
    /// GradesDataDifferences
    public static final String FILE_NAME_GRADES_DATA_DIFFERENCES = "GradesDataDifferences";
    public static final String GRADES_ADDED = "GRADES_ADDED";
    public static final String GRADES_MODIFIED = "GRADES_MODIFIED";
    public static final String GRADES_REMOVED = "GRADES_REMOVED";
}

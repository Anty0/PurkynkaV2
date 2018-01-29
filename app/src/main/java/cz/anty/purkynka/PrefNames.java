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
    /// UpdateData
    public static final String FILE_NAME_UPDATE_DATA = "UpdateData";
    public static final String SCHEDULE_VERSION = "SCHEDULE_VERSION";
    public static final String LATEST_VERSION_CODE = "LATEST_VERSION_CODE";
    public static final String LATEST_VERSION_NAME = "LATEST_VERSION_NAME";
    /// ActiveAccountManager
    public static final String FILE_NAME_ACTIVE_ACCOUNT_DATA = "ActiveAccountData";
    public static final String ACTIVE_ACCOUNT_NAME = "ACTIVE_ACCOUNT_NAME";
    /// SettingsData
    public static final String FILE_NAME_SETTINGS_DATA = "SettingsData";
    // Grades
    /// GradesData
    public static final String FILE_NAME_GRADES_DATA = "GradesData";
    public static final String FIRST_SYNC = "FIRST_SYNC";
    public static final String SYNC_RESULT = "SYNC_RESULT";
    public static final String GRADES_MAP = "GRADES";
    /// GradesLoginData
    public static final String FILE_NAME_GRADES_LOGIN_DATA = "GradesLoginData";
    /// GradesUiData
    public static final String FILE_NAME_GRADES_UI_DATA = "GradesUI";
    public static final String LAST_SORT_GRADES = "SORT_GRADES";
    // WifiLogin
    /// WifiData
    public static final String FILE_NAME_WIFI_DATA = "WifiData";
    public static final String LOGIN_COUNTER = "LOGIN_COUNTER";
    /// WifiLoginData
    public static final String FILE_NAME_WIFI_LOGIN_DATA = "WifiLoginData";
}

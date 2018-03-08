/*
 * app
 * Copyright (C)   2018  anty
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

package cz.anty.purkynka.utils

/**
 * @author anty
 */

// Other
/// AppPreferences
const val FILE_NAME_APP_PREFERENCES = "AppPreferences"
const val ENABLE_DEBUG_MODE = "ENABLE_DEBUG_MODE"
const val SHOW_WELCOME_ITEM = "SHOW_WELCOME_ITEM"
/// UpdateData
const val FILE_NAME_UPDATE_DATA = "UpdateData"
const val SCHEDULE_VERSION = "SCHEDULE_VERSION"
const val LATEST_VERSION = "LATEST_VERSION"
const val LAST_KNOWN_VERSION = "LAST_KNOWN_VERSION_CODE"
/// ActiveAccountManager
const val FILE_NAME_ACTIVE_ACCOUNT_DATA = "ActiveAccountData"
const val ACTIVE_ACCOUNT_ID = "ACTIVE_ACCOUNT_ID"
/// FeedbackData
const val FILE_NAME_FEEDBACK_DATA = "FeedbackData"
const val LAST_ERROR_VERSION = "LAST_ERROR_VERSION"


// Grades
/// GradesData
const val FILE_NAME_GRADES_DATA = "GradesData"
const val FIRST_SYNC = "FIRST_SYNC"
const val SYNC_RESULT = "SYNC_RESULT"
const val GRADES_MAP = "GRADES"
/// GradesLoginData
const val FILE_NAME_GRADES_LOGIN_DATA = "GradesLoginData"
/// GradesUiData
const val FILE_NAME_GRADES_UI_DATA = "GradesUI"
const val LAST_SORT_GRADES = "SORT_GRADES"
/// GradesPreferences
const val FILE_NAME_GRADES_PREFERENCES = "GradesPreferences"
const val GRADES_BAD_AVERAGE = "GRADES_BAD_AVERAGE"
const val WIDGET_ACCOUNT_ID = "WIDGET_ACCOUNT_ID"
const val WIDGET_SORT = "WIDGET_SORT"

// WifiLogin
/// WifiData
const val FILE_NAME_WIFI_DATA = "WifiData"
const val LOGIN_COUNTER = "LOGIN_COUNTER"
/// WifiLoginData
const val FILE_NAME_WIFI_LOGIN_DATA = "WifiLoginData"

// Lunches
/// LunchesData    // + FIRST_SYNC SYNC_RESULT
const val FILE_NAME_LUNCHES_DATA = "LunchesData"
const val LUNCHES_LIST = "LUNCHES"
const val CREDIT = "CREDIT"
const val INVALID = "INVALID"
/// LunchesLoginData
const val FILE_NAME_LUNCHES_LOGIN_DATA = "LunchesLoginData"
/// LunchesPreferences    // + WIDGET_ACCOUNT_ID
const val FILE_NAME_LUNCHES_PREFERENCES = "LunchesPreferences"
const val SHOW_DASHBOARD_CREDIT_WARNING = "SHOW_DASHBOARD_CREDIT_WARNING"

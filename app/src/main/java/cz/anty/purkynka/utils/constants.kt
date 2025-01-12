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

import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import cz.anty.purkynka.BuildConfig

/**
 * @author anty
 */

const val APP_OLD_PACKAGE_NAME = "cz.anty.purkynkamanager"

//---------------------------------------------------------------------

const val AUTHORITY_PROVIDER_FILES = "${BuildConfig.APPLICATION_ID}.files"

//---------------------------------------------------------------------

val ICON_HOME_DASHBOARD = GoogleMaterial.Icon.gmd_home
val ICON_GRADES = GoogleMaterial.Icon.gmd_book
val ICON_WIFI_LOGIN = GoogleMaterial.Icon.gmd_wifi_lock
val ICON_LUNCHES = GoogleMaterial.Icon.gmd_restaurant
val ICON_ATTENDANCE = GoogleMaterial.Icon.gmd_people

val ICON_LUNCHES_ORDER = GoogleMaterial.Icon.gmd_shopping_basket
val ICON_LUNCHES_BURZA = GoogleMaterial.Icon.gmd_attach_money
val ICON_LUNCHES_BURZA_WATCHER = CommunityMaterial.Icon.cmd_auto_fix

val ICON_FACEBOOK = CommunityMaterial.Icon.cmd_facebook_box
val ICON_WEB = GoogleMaterial.Icon.gmd_public
val ICON_DONATE = GoogleMaterial.Icon.gmd_monetization_on

val ICON_UPDATE = GoogleMaterial.Icon.gmd_system_update

//---------------------------------------------------------------------

const val URL_FACEBOOK_PAGE = "https://www.facebook.com/aplikacepurkynka/" // TODO: 6/16/17 add url; use firebase remote config
const val URL_WEB_PAGE = "https://anty.codetopic.eu/purkynka/" // TODO: 6/16/17 add url; use firebase remote config
//const val URL_WEB_DONATE_PAGE = "https://anty.codetopic.eu/purkynka/" // TODO: 6/16/17 add url; use firebase remote config

//---------------------------------------------------------------------

const val FBA_ACCOUNT_CREATE = "purkynka_account_create"
const val FBA_ACCOUNT_EDIT = "purkynka_account_edit"
const val FBA_EASTER_EGG_CLICKED = "purkynka_easter_egg_clicked"
const val FBA_ATTENDANCE_SEARCH = "purkynka_attendance_search"
const val FBA_GRADES_LOGIN = "purkynka_grades_login"
const val FBA_GRADES_LOGOUT = "purkynka_grades_logout"
const val FBA_LUNCHES_LOGIN = "purkynka_lunches_login"
const val FBA_LUNCHES_LOGOUT = "purkynka_lunches_logout"
const val FBA_WIFI_ENABLE = "purkynka_wifi_enable"
const val FBA_WIFI_DISABLE = "purkynka_wifi_disable"
const val FBA_UPDATE_DOWNLOAD = "purkynka_update_download"
const val FBA_UPDATE_INSTALL = "purkynka_update_install"

//---------------------------------------------------------------------

const val FBP_GRADES_SYNC = "purkynka_grades_sync"

//---------------------------------------------------------------------

const val CONNECTION_TIMEOUT_SAS: Int = 1000 * 60 * 2 // 2 minutes in milliseconds

const val SYNC_FREQUENCY_GRADES: Long = 60 * 60 * 1 // 1 hour in seconds
const val SYNC_FREQUENCY_LUNCHES: Long = 60 * 60 * 3 // 3 hours in seconds

//---------------------------------------------------------------------

// Helpers
const val DASHBOARD_PRIORITY_WELCOME: Int = 14_000_000
const val DASHBOARD_PRIORITY_BROADCAST_REJECTION_WARNING: Int = 13_000_000

// System
const val DASHBOARD_PRIORITY_UPDATE_AVAILABLE: Int = 12_000_000
const val DASHBOARD_PRIORITY_VERSION_CHANGES: Int = 10_000_000 // AND -changesVersionCode
const val DASHBOARD_PRIORITY_ERROR_FEEDBACK: Int = 8_000_000

// Other
const val DASHBOARD_PRIORITY_LUNCHES_CREDIT: Int = 6_000_000
const val DASHBOARD_PRIORITY_LUNCHES_NEXT_LUNCH: Int = 5_000_000
const val DASHBOARD_PRIORITY_LUNCHES_NEW: Int = 4_000_000 // AND -date
const val DASHBOARD_PRIORITY_GRADES_NEW: Int = 3_000_000
const val DASHBOARD_PRIORITY_GRADES_SUBJECTS_AVERAGE_BAD: Int = 2_000_000 // AND +(average * 100).toInt()

// Login
const val DASHBOARD_PRIORITY_LOGIN_GRADES: Int = 900_000
const val DASHBOARD_PRIORITY_LOGIN_LUNCHES: Int = 800_000
const val DASHBOARD_PRIORITY_LOGIN_WIFI: Int = 700_000
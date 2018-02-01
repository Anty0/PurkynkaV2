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

package cz.anty.purkynka

import android.accounts.AccountManager
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.design.widget.NavigationView
import eu.codetopic.utils.ui.activity.navigation.NavigationActivity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import eu.codetopic.utils.Constants.ICON_DEBUG
import eu.codetopic.utils.Constants.ICON_SETTINGS
import cz.anty.purkynka.Constants.ICON_ATTENDANCE
import cz.anty.purkynka.Constants.ICON_DONATE
import cz.anty.purkynka.Constants.ICON_FACEBOOK
import cz.anty.purkynka.Constants.ICON_GRADES
import cz.anty.purkynka.Constants.ICON_HOME_DASHBOARD
import cz.anty.purkynka.Constants.ICON_LUNCHES
import cz.anty.purkynka.Constants.ICON_LUNCHES_BURZA
import cz.anty.purkynka.Constants.ICON_LUNCHES_BURZA_WATCHER
import cz.anty.purkynka.Constants.ICON_LUNCHES_ORDER
import cz.anty.purkynka.Constants.ICON_TIMETABLES
import cz.anty.purkynka.Constants.ICON_WEB
import cz.anty.purkynka.Constants.ICON_WIFI_LOGIN
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.account.save.ActiveAccount
import cz.anty.purkynka.account.ui.AccountEditActivity
import cz.anty.purkynka.attendance.AttendanceSearchFragment
import cz.anty.purkynka.dashboard.DashboardFragment
import cz.anty.purkynka.debug.DebugActivity
import cz.anty.purkynka.grades.GradesFragment
import cz.anty.purkynka.lunches.LunchesBurzaFragment
import cz.anty.purkynka.lunches.LunchesBurzaWatcherFragment
import cz.anty.purkynka.lunches.LunchesLoginFragment
import cz.anty.purkynka.lunches.LunchesOrderFragment
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.settings.SettingsActivity
import cz.anty.purkynka.timetables.TimetablesListFragment
import cz.anty.purkynka.wifilogin.WifiLoginFragment
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.AndroidExtensions.getIconics
import eu.codetopic.utils.AndroidUtils
import eu.codetopic.utils.broadcast.LocalBroadcast


class MainActivity : NavigationActivity() {

    companion object {
        private const val LOG_TAG = "MainActivity"

        private const val REQUEST_CODE_EDIT_ACCOUNT: Int = 1

        private const val EXTRA_FRAGMENT_CLASS = "cz.anty.purkynka.$LOG_TAG.EXTRA_FRAGMENT_CLASS"
        private const val EXTRA_FRAGMENT_EXTRAS = "cz.anty.purkynka.$LOG_TAG.EXTRA_FRAGMENT_EXTRAS"

        @JvmOverloads
        fun getStartIntent(context: Context, fragmentClass: Class<out Fragment>? = null,
                           fragmentExtras: Bundle? = null): Intent =
                Intent(context, MainActivity::class.java)
                        .putExtra(EXTRA_FRAGMENT_CLASS, fragmentClass)
                        .putExtra(EXTRA_FRAGMENT_EXTRAS, fragmentExtras)

        @JvmOverloads
        fun start(context: Context, fragmentClass: Class<out Fragment>? = null,
                  fragmentExtras: Bundle? = null) =
                context.startActivity(getStartIntent(context, fragmentClass, fragmentExtras))
    }

    override val mainFragmentClass: Class<out Fragment>?
        get() = DashboardFragment::class.java

    private val activeAccountChangeReceiver: BroadcastReceiver =
            broadcast { _, _ -> invalidateNavigationMenu() }

    private val accountsChangeReceiver: BroadcastReceiver =
            broadcast { _, _ -> invalidateNavigationMenu() }

    private val lunchesLoginDataChangeReceiver: BroadcastReceiver =
            broadcast { _, _ -> invalidateNavigationMenu() }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar) // TODO: Animated app splash screen
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) processIntent(intent)

        enableSwitchingAccounts = true
        enableActiveAccountEditButton = true

        val iconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_foreground)
        val iconWidth = iconBitmap.width
        val iconHeight = iconBitmap.height
        val cropWidth = iconWidth / 2
        val cropHeight = iconHeight / 2
        val cropStartX = (iconWidth - cropWidth) / 2
        val cropStartY = (iconHeight - cropHeight) / 2
        val croppedIconBitmap = Bitmap.createBitmap(iconBitmap,
                cropStartX, cropStartY, cropWidth, cropHeight)
        setNavigationViewAppIconBitmap(croppedIconBitmap)

        findViewById<NavigationView>(R.id.navigationView).apply {
            val white = ContextCompat.getColorStateList(this@MainActivity, android.R.color.white)
            itemTextColor = white
            itemIconTintList = white
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        @Suppress("UNCHECKED_CAST")
        (intent?.getSerializableExtra(EXTRA_FRAGMENT_CLASS) as Class<out Fragment>?)
                ?.let { replaceFragment(it, intent?.getBundleExtra(EXTRA_FRAGMENT_EXTRAS)) }
    }

    override fun onResume() {
        super.onResume()

        registerReceiver(
                accountsChangeReceiver,
                intentFilter(
                        Accounts.ACTION_ACCOUNTS_CHANGED,
                        ActiveAccount.getter
                )
        )
        LocalBroadcast.registerReceiver(
                activeAccountChangeReceiver,
                intentFilter(ActiveAccount.getter)
        )
        LocalBroadcast.registerReceiver(
                lunchesLoginDataChangeReceiver,
                intentFilter(LunchesLoginData.getter)
        )

        invalidateNavigationMenu()
    }

    override fun onPause() {
        LocalBroadcast.unregisterReceiver(lunchesLoginDataChangeReceiver)
        LocalBroadcast.unregisterReceiver(activeAccountChangeReceiver)
        unregisterReceiver(accountsChangeReceiver)

        super.onPause()
    }

    override fun onCreateAccountNavigationMenu(menu: Menu): Boolean {
        super.onCreateAccountNavigationMenu(menu)
        menuInflater.inflate(R.menu.activity_main_accounts, menu)
        val accounts = Accounts.getAll(this)
        val activeAccount = ActiveAccount.get()
        accounts.forEach {
            if (it == activeAccount) return@forEach
            menu.add(R.id.menu_group_main, R.id.menu_item_account, Menu.NONE, it.name)
                    .apply {
                        icon = getIconics(GoogleMaterial.Icon.gmd_account_circle)
                                .actionBar()
                    }
        }
        menu.add(R.id.menu_group_main, R.id.menu_item_add_account, Menu.NONE, R.string.action_add_user)
                .apply {
                    icon = getIconics(GoogleMaterial.Icon.gmd_add)
                            .actionBar()
                }
        return true
    }

    override fun onUpdateSelectedAccountNavigationMenuItem(currentFragment: Fragment?,
                                                           menu: Menu): Boolean {
        return false
    }

    override fun onAccountNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_add_account -> Accounts.requestAdd(this)
            R.id.menu_item_account -> ActiveAccount.set(item.title.toString())
            else -> return super.onAccountNavigationItemSelected(item)
        }
        return true
    }

    override fun onUpdateActiveAccountName(): CharSequence {
        return ActiveAccount.get()?.name ?: ""
    }

    override fun onEditAccountButtonClick(v: View): Boolean {
        startActivityForResult(
                Intent(this, AccountEditActivity::class.java)
                        .putExtra(AccountEditActivity.KEY_ACCOUNT,
                                ActiveAccount.get() ?: return false),
                REQUEST_CODE_EDIT_ACCOUNT)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_EDIT_ACCOUNT) {
            if (resultCode != Activity.RESULT_OK || data == null) return
            ActiveAccount.set(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateNavigationMenu(menu: Menu): Boolean {
        super.onCreateNavigationMenu(menu)

        menuInflater.inflate(R.menu.activity_main_drawer, menu)

        menu.findItem(R.id.nav_dashboard).icon =
                getIconics(ICON_HOME_DASHBOARD).actionBar()
        menu.findItem(R.id.nav_grades).icon =
                getIconics(ICON_GRADES).actionBar()
        menu.findItem(R.id.nav_wifi_login).icon =
                getIconics(ICON_WIFI_LOGIN).actionBar()
        menu.findItem(R.id.nav_timetables).icon =
                getIconics(ICON_TIMETABLES).actionBar()
        menu.findItem(R.id.nav_attendance).icon =
                getIconics(ICON_ATTENDANCE).actionBar()

        val loggedInLunches = ActiveAccount.getId()
                ?.let { LunchesLoginData.loginData.isLoggedIn(it) } ?: false
        menu.findItem(R.id.nav_lunches_login).apply {
            icon = getIconics(ICON_LUNCHES).actionBar()
            isVisible = !loggedInLunches
        }
        menu.findItem(R.id.nav_lunches).apply {
            icon = getIconics(ICON_LUNCHES).actionBar()
            isVisible = loggedInLunches
        }
        menu.findItem(R.id.nav_lunches_order).icon =
                getIconics(ICON_LUNCHES_ORDER).actionBar()
        menu.findItem(R.id.nav_lunches_burza).icon =
                getIconics(ICON_LUNCHES_BURZA).actionBar()
        menu.findItem(R.id.nav_lunches_burza_watcher).icon =
                getIconics(ICON_LUNCHES_BURZA_WATCHER).actionBar()

        menu.findItem(R.id.nav_settings).icon =
                getIconics(ICON_SETTINGS).actionBar()
        menu.findItem(R.id.nav_debug).apply {
            icon = getIconics(ICON_DEBUG).actionBar()
            isVisible = BuildConfig.DEBUG
        }

        menu.findItem(R.id.nav_contact_facebook).icon =
                getIconics(ICON_FACEBOOK).actionBar()
        menu.findItem(R.id.nav_contact_web_page).icon =
                getIconics(ICON_WEB).actionBar()
        menu.findItem(R.id.nav_contact_web_page_donate).icon =
                getIconics(ICON_DONATE).actionBar()

        return true
    }

    override fun onUpdateSelectedNavigationMenuItem(currentFragment: Fragment?, menu: Menu): Boolean {
        menu.setGroupVisible(R.id.menu_group_lunches, false)

        if (currentFragment == null) {
            return super.onUpdateSelectedNavigationMenuItem(null, menu)
        }

        with (menu) {
            when (currentFragment.javaClass) {
                DashboardFragment::class.java -> findItem(R.id.nav_dashboard).isChecked = true
                GradesFragment::class.java -> findItem(R.id.nav_grades).isChecked = true
                WifiLoginFragment::class.java -> findItem(R.id.nav_wifi_login).isChecked = true
                TimetablesListFragment::class.java -> findItem(R.id.nav_timetables).isChecked = true
                AttendanceSearchFragment::class.java -> findItem(R.id.nav_attendance).isChecked = true
                LunchesLoginFragment::class.java -> findItem(R.id.nav_lunches_login).isChecked = true
                LunchesOrderFragment::class.java -> findItem(R.id.nav_lunches_order).isChecked = true
                LunchesBurzaFragment::class.java -> findItem(R.id.nav_lunches_burza).isChecked = true
                LunchesBurzaWatcherFragment::class.java -> findItem(R.id.nav_lunches_burza_watcher).isChecked = true
                else -> return super.onUpdateSelectedNavigationMenuItem(currentFragment, menu)
            }
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> replaceFragment(DashboardFragment::class.java)
            R.id.nav_grades -> replaceFragment(GradesFragment::class.java)
            R.id.nav_wifi_login -> replaceFragment(WifiLoginFragment::class.java)
            R.id.nav_timetables -> replaceFragment(TimetablesListFragment::class.java)
            R.id.nav_attendance -> replaceFragment(AttendanceSearchFragment::class.java)
            R.id.nav_lunches_login -> replaceFragment(LunchesLoginFragment::class.java)
            R.id.nav_lunches_order -> replaceFragment(LunchesOrderFragment::class.java)
            R.id.nav_lunches_burza -> replaceFragment(LunchesBurzaFragment::class.java)
            R.id.nav_lunches_burza_watcher -> replaceFragment(LunchesBurzaWatcherFragment::class.java)
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.nav_debug -> startActivity(Intent(this, DebugActivity::class.java))
            R.id.nav_contact_facebook -> AndroidUtils.openUri(this, Constants.URL_FACEBOOK_PAGE, R.string.toast_browser_failed)
            R.id.nav_contact_web_page -> AndroidUtils.openUri(this, Constants.URL_WEB_PAGE, R.string.toast_browser_failed)
            R.id.nav_contact_web_page_donate -> AndroidUtils.openUri(this, Constants.URL_WEB_DONATE_PAGE, R.string.toast_browser_failed)
            else -> return super.onNavigationItemSelected(item)
        }
        return true
    }

    override fun onBeforeReplaceFragment(ft: FragmentTransaction, fragment: Fragment?) {
        ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
    }
}

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

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.design.widget.NavigationView
import eu.codetopic.utils.ui.activity.navigation.NavigationActivity
import android.support.design.widget.TabLayout
import android.content.Intent
import android.os.Build
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.Menu
import android.view.MenuItem
import android.view.View
import cz.anty.purkynka.accounts.ui.AccountEditActivity
import cz.anty.purkynka.accounts.AccountsHelper
import cz.anty.purkynka.accounts.ActiveAccountManager
import cz.anty.purkynka.dashboard.DashboardFragment
import cz.anty.purkynka.debug.DebugActivity
import cz.anty.purkynka.grades.GradesFragment
import cz.anty.purkynka.settings.SettingsActivity
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.AndroidUtils
import eu.codetopic.utils.broadcast.LocalBroadcast


class MainActivity : NavigationActivity() {

    companion object {
        private const val LOG_TAG = "MainActivity"

        private const val REQUEST_CODE_EDIT_ACCOUNT: Int = 1

        private const val EXTRA_FRAGMENT_CLASS = "cz.anty.purkynka.$LOG_TAG.EXTRA_FRAGMENT_CLASS"
        private const val EXTRA_FRAGMENT_EXTRAS = "cz.anty.purkynka.$LOG_TAG.EXTRA_FRAGMENT_EXTRAS"

        @JvmOverloads
        fun getStartIntent(context: Context, fragmentClass: Class<out Fragment>? = null, fragmentExtras: Bundle? = null): Intent =
                Intent(context, MainActivity::class.java)
                        .putExtra(EXTRA_FRAGMENT_CLASS, fragmentClass)
                        .putExtra(EXTRA_FRAGMENT_EXTRAS, fragmentExtras)

        @JvmOverloads
        fun start(context: Context, fragmentClass: Class<out Fragment>? = null, fragmentExtras: Bundle? = null) =
                context.startActivity(getStartIntent(context, fragmentClass, fragmentExtras))
    }

    override val mainFragmentClass: Class<out Fragment>?
        get() = DashboardFragment::class.java

    private val accountChangedReceiver: AccountChangeReceiver = AccountChangeReceiver()
    private val activeAccountManager: ActiveAccountManager = ActiveAccountManager.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar) // TODO: Animated app logo
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) processIntent(intent)

        enableSwitchingAccounts = true
        enableActiveAccountEditButton = true

        setNavigationViewAppIconResource(R.mipmap.ic_launcher)

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

        with (AccountManager.get(this)) {
            if (Build.VERSION.SDK_INT >= 26) addOnAccountsUpdatedListener(accountChangedReceiver,
                    null, true, arrayOf(AccountsHelper.ACCOUNT_TYPE))
            else addOnAccountsUpdatedListener(accountChangedReceiver, null, true)
        }
        LocalBroadcast.registerReceiver(accountChangedReceiver, intentFilter(ActiveAccountManager.getter))

        invalidateNavigationMenu()
    }

    override fun onPause() {
        LocalBroadcast.unregisterReceiver(accountChangedReceiver)
        AccountManager.get(this).removeOnAccountsUpdatedListener(accountChangedReceiver)

        super.onPause()
    }

    override fun onCreateAccountNavigationMenu(menu: Menu): Boolean {
        super.onCreateAccountNavigationMenu(menu)
        menuInflater.inflate(R.menu.activity_main_accounts, menu)
        val accounts = AccountsHelper.getAllAccounts(this)
        val activeAccount = activeAccountManager.activeAccount
        accounts.forEach {
            if (it == activeAccount) return@forEach
            menu.add(R.id.menu_group_main, 0, Menu.NONE, it.name)
                    .setIcon(R.drawable.ic_account)
        }
        menu.add(R.id.menu_group_main, 0, Menu.NONE, R.string.action_add_user)
                .setIcon(R.drawable.ic_action_add)
        return true
    }

    override fun onUpdateSelectedAccountNavigationMenuItem(currentFragment: Fragment?, menu: Menu): Boolean {
        return false
    }

    override fun onAccountNavigationItemSelected(item: MenuItem): Boolean {
        if (item.title == getText(R.string.action_add_user)) {
            AccountManager.get(this).addAccount(AccountsHelper.ACCOUNT_TYPE, null,
                    null, null, this, null, null)
            return true
        }
        activeAccountManager.setActiveAccount(item.title.toString())
        return true
    }

    override fun onUpdateActiveAccountName(): CharSequence {
        return activeAccountManager.activeAccount?.name ?: ""
    }

    override fun onEditAccountButtonClick(v: View): Boolean {
        startActivityForResult(
                Intent(this, AccountEditActivity::class.java)
                        .putExtra(AccountEditActivity.KEY_ACCOUNT,
                                activeAccountManager.activeAccount ?: return false),
                REQUEST_CODE_EDIT_ACCOUNT)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_EDIT_ACCOUNT) {
            if (resultCode != Activity.RESULT_OK || data == null) return
            activeAccountManager.setActiveAccount(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateNavigationMenu(menu: Menu): Boolean {
        super.onCreateNavigationMenu(menu)
        menuInflater.inflate(R.menu.activity_main_drawer, menu)
        menu.findItem(R.id.nav_debug).isVisible = BuildConfig.DEBUG
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
                // WifiLoginFragment::class.java -> findItem(R.id.nav_wifi_login).isChecked = true
                /*LunchesFragment::class.java, LunchesOrderFragment::class.java,
                LunchesBurzaFragment::class.java, LunchesBurzaWatcherFragment::class.java -> {
                    findItem(R.id.nav_lunches).isChecked = true
                    setGroupVisible(R.id.menu_group_lunches, true)
                    when (currentFragment.javaClass) {
                        LunchesOrderFragment::class.java -> findItem(R.id.nav_lunches_order).isChecked = true
                        LunchesBurzaFragment::class.java -> findItem(R.id.nav_lunches_burza).isChecked = true
                        LunchesBurzaWatcherFragment::class.java -> findItem(R.id.nav_lunches_burza_watcher).isChecked = true
                        else -> {
                            findItem(R.id.nav_lunches_order).isChecked = false
                            findItem(R.id.nav_lunches_burza).isChecked = false
                            findItem(R.id.nav_lunches_burza_watcher).isChecked = false
                        }
                    }
                }*/
                // TimetablesFragment::class.java -> findItem(R.id.nav_timetables).isChecked = true
                // AttendanceFragment::class.java -> findItem(R.id.nav_attendance).isChecked = true
                else -> return super.onUpdateSelectedNavigationMenuItem(currentFragment, menu)
            }
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> replaceFragment(DashboardFragment::class.java)
            R.id.nav_grades -> replaceFragment(GradesFragment::class.java)
            // R.id.nav_wifi_login -> replaceFragment(WifiLoginFragment::class.java)
            // R.id.nav_lunches -> replaceFragment(LunchesFragment::class.java)
            // R.id.nav_lunches_order -> replaceFragment(LunchesOrderFragment::class.java)
            // R.id.nav_lunches_burza -> replaceFragment(LunchesBurzaFragment::class.java)
            // R.id.nav_lunches_burza_watcher -> replaceFragment(LunchesBurzaWatcherFragment::class.java)
            // R.id.nav_timetables -> replaceFragment(TimetablesFragment::class.java)
            // R.id.nav_attendance -> replaceFragment(AttendanceFragment::class.java)
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
        val tabLayout = window.findViewById<TabLayout>(R.id.tabLayout)
        if (tabLayout != null) {
            tabLayout.visibility = View.GONE
        }
    }

    inner class AccountChangeReceiver : BroadcastReceiver(), OnAccountsUpdateListener {
        override fun onAccountsUpdated(accounts: Array<out Account>?) {
            super@MainActivity.invalidateNavigationMenu()
        }

        override fun onReceive(context: Context, intent: Intent) {
            super@MainActivity.invalidateNavigationMenu()
        }
    }
}

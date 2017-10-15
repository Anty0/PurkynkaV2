package cz.anty.purkynka

import android.accounts.AccountManager
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.design.widget.NavigationView
import eu.codetopic.utils.ui.activity.navigation.NavigationActivity
import android.support.design.widget.TabLayout
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.LocalBroadcastManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import cz.anty.purkynka.accounts.ActiveAccountManager
import cz.anty.purkynka.dashboard.DashboardFragment
import cz.anty.purkynka.marks.MarksFragment
import cz.anty.purkynka.settings.SettingsActivity
import eu.codetopic.utils.AndroidUtils
import eu.codetopic.utils.data.preferences.PreferencesData


class MainActivity : NavigationActivity() {

    private val LOG_TAG = "MainActivity"

    private var mAccountChangedReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)

        mAccountChangedReceiver = AccountChangeReceiver()
        registerReceiver(mAccountChangedReceiver, IntentFilter(AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION)) // TODO: use new api
        LocalBroadcastManager.getInstance(this).registerReceiver(mAccountChangedReceiver,
                IntentFilter(PreferencesData.getBroadcastActionChanged(ActiveAccountManager.getter)))

        isEnableSwitchingAccounts = true

        setNavigationViewAppIconResource(R.mipmap.ic_launcher_round)

        val navView = findViewById<NavigationView>(R.id.nav_view)
        if (navView != null) {
            navView.itemTextColor = ContextCompat.getColorStateList(this, android.R.color.white)
            navView.itemIconTintList = ContextCompat.getColorStateList(this, android.R.color.white)
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAccountChangedReceiver)
        unregisterReceiver(mAccountChangedReceiver)
        mAccountChangedReceiver = null
        super.onDestroy()
    }

    override fun getMainFragmentClass(): Class<out Fragment> {
        return DashboardFragment::class.java
    }

    override fun onCreateAccountNavigationMenu(menu: Menu): Boolean {
        super.onCreateAccountNavigationMenu(menu)
        menuInflater.inflate(R.menu.activity_main_accounts, menu)
        val accountManager = ActiveAccountManager.getter.get()
        val accounts = accountManager.availableAccounts
        val activeAccount = accountManager.activeAccount
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
            // TODO: implement
            // AccountManager.get(this).addAccount(AccountsHelper.ACCOUNT_TYPE, )
            return true
        }
        ActiveAccountManager.getter.get().setActiveAccount(item.title.toString())
        return true
    }

    override fun onUpdateActiveAccountName(): CharSequence {
        return ActiveAccountManager.getter.get().activeAccount?.name ?: ""
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
                MarksFragment::class.java -> findItem(R.id.nav_marks).isChecked = true
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
            R.id.nav_marks -> replaceFragment(MarksFragment::class.java)
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

    override fun onBeforeReplaceFragment(ft: FragmentTransaction, fragment: Fragment) {
        ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        val tabLayout = window.findViewById<TabLayout>(R.id.tabLayout)
        if (tabLayout != null) {
            tabLayout.visibility = View.GONE
        }
    }

    inner class AccountChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            super@MainActivity.invalidateNavigationMenu()
        }
    }
}

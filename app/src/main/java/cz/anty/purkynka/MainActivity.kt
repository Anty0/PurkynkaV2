package cz.anty.purkynka

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.design.widget.NavigationView
import eu.codetopic.utils.ui.activity.navigation.NavigationActivity
import android.support.design.widget.TabLayout
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.Menu
import android.view.MenuItem
import android.view.View
import cz.anty.purkynka.dashboard.DashboardFragment
import cz.anty.purkynka.marks.MarksFragment
import cz.anty.purkynka.settings.SettingsActivity
import eu.codetopic.java.utils.log.Log


class MainActivity : NavigationActivity() {

    private val LOG_TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)

        setNavigationViewAppIconResource(R.mipmap.ic_launcher_round)

        val navView = findViewById<NavigationView>(R.id.nav_view)
        if (navView != null) {
            navView.itemTextColor = ContextCompat.getColorStateList(this, android.R.color.white)
            navView.itemIconTintList = ContextCompat.getColorStateList(this, android.R.color.white)
        }
    }

    override fun getMainFragmentClass(): Class<out Fragment> {
        return DashboardFragment::class.java
    }

    override fun onUpdateSelectedNavigationMenuItem(currentFragment: Fragment?, menu: Menu): Boolean {
        menu.setGroupVisible(R.id.menu_group_lunches, false)

        if (currentFragment == null) {
            return super.onUpdateSelectedNavigationMenuItem(null, menu)
        }

        val fragmentClass = currentFragment.javaClass

        if (fragmentClass == DashboardFragment::class.java) {
            menu.findItem(R.id.nav_dashboard).isChecked = true
            return true
        }
        if (fragmentClass == MarksFragment::class.java) {
            menu.findItem(R.id.nav_marks).isChecked = true
            return true
        }
        /*if (fragmentClass.equals(WifiLoginFragment.class)) {
            menu.findItem(R.id.nav_wifi_login).setChecked(true);
            return true;
        }
        if (fragmentClass.equals(LunchesFragment.class)
                || fragmentClass.equals(LunchesOrderFragment.class)
                || fragmentClass.equals(LunchesBurzaFragment.class)
                || fragmentClass.equals(LunchesBurzaWatcherFragment.class)) {
            menu.findItem(R.id.nav_lunches).setChecked(true);
            menu.setGroupVisible(R.id.menu_group_lunches, true);

            if (fragmentClass.equals(LunchesOrderFragment.class)) {
                menu.findItem(R.id.nav_lunches_order).setChecked(true);
            } else if (fragmentClass.equals(LunchesBurzaFragment.class)) {
                menu.findItem(R.id.nav_lunches_burza).setChecked(true);
            } else if (fragmentClass.equals(LunchesBurzaWatcherFragment.class)) {
                menu.findItem(R.id.nav_lunches_burza_watcher).setChecked(true);
            } else {
                menu.findItem(R.id.nav_lunches_order).setChecked(false);
                menu.findItem(R.id.nav_lunches_burza).setChecked(false);
                menu.findItem(R.id.nav_lunches_burza_watcher).setChecked(false);
            }
            return true;
        }
        if (fragmentClass.equals(TimetablesFragment.class)) {
            menu.findItem(R.id.nav_timetables).setChecked(true);
            return true;
        }
        if (fragmentClass.equals(AttendanceFragment.class)) {
            menu.findItem(R.id.nav_attendance).setChecked(true);
            return true;
        }*/

        return super.onUpdateSelectedNavigationMenuItem(currentFragment, menu)
    }

    override fun onCreateNavigationMenu(menu: Menu): Boolean {
        super.onCreateNavigationMenu(menu)
        menuInflater.inflate(R.menu.activity_main_drawer, menu)
        if (BuildConfig.DEBUG) {
            menu.findItem(R.id.nav_debug).isVisible = true
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.nav_dashboard) {
            replaceFragment(DashboardFragment::class.java)
            return true
        } else if (id == R.id.nav_marks) {
            replaceFragment(MarksFragment::class.java)
            return true
        } else /*if (id == R.id.nav_wifi_login) {
            replaceFragment(WifiLoginFragment.class);
            return true;
        } else if (id == R.id.nav_lunches) {
            replaceFragment(LunchesFragment.class);
            return true;
        } else if (id == R.id.nav_lunches_order) {
            replaceFragment(LunchesOrderFragment.class);
            return true;
        } else if (id == R.id.nav_lunches_burza) {
            replaceFragment(LunchesBurzaFragment.class);
            return true;
        } else if (id == R.id.nav_lunches_burza_watcher) {
            replaceFragment(LunchesBurzaWatcherFragment.class);
            return true;
        } else if (id == R.id.nav_timetables) {
            replaceFragment(TimetablesFragment.class);
            return true;
        } else if (id == R.id.nav_attendance) {
            replaceFragment(AttendanceFragment.class);
            return true;
        } else*/ if (id == R.id.nav_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        } else if (id == R.id.nav_debug) {
            startActivity(Intent(this, DebugActivity::class.java))
            return true
        } else if (id == R.id.nav_contact_facebook) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_FACEBOOK_PAGE)))
            } catch (e: Exception) {
                Log.w(LOG_TAG, e)
                Toast.makeText(this, R.string.toast_browser_failed,
                        Toast.LENGTH_SHORT).show()
            }
            return true
        } else if (id == R.id.nav_contact_web_page) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_WEB_PAGE)))
            } catch (e: Exception) {
                Log.w(LOG_TAG, e)
                Toast.makeText(this, R.string.toast_browser_failed,
                        Toast.LENGTH_SHORT).show()
            }
            return true
        } else if (id == R.id.nav_contact_web_page) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_WEB_PAGE_DONATE)))
            } catch (e: Exception) {
                Log.w(LOG_TAG, e)
                Toast.makeText(this, R.string.toast_browser_failed,
                        Toast.LENGTH_SHORT).show()
            }
            return true
        }

        return super.onNavigationItemSelected(item)
    }

    override fun onBeforeReplaceFragment(ft: FragmentTransaction, fragment: Fragment) {
        ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        val tabLayout = window.findViewById<TabLayout>(R.id.tabLayout)
        if (tabLayout != null) {
            tabLayout.visibility = View.GONE
        }
    }
}

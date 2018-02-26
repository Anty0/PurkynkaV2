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
import cz.anty.purkynka.utils.ICON_ATTENDANCE
import cz.anty.purkynka.utils.ICON_DONATE
import cz.anty.purkynka.utils.ICON_FACEBOOK
import cz.anty.purkynka.utils.ICON_GRADES
import cz.anty.purkynka.utils.ICON_HOME_DASHBOARD
import cz.anty.purkynka.utils.ICON_LUNCHES
import cz.anty.purkynka.utils.ICON_LUNCHES_BURZA
import cz.anty.purkynka.utils.ICON_LUNCHES_BURZA_WATCHER
import cz.anty.purkynka.utils.ICON_LUNCHES_ORDER
import cz.anty.purkynka.utils.ICON_WEB
import cz.anty.purkynka.utils.ICON_WIFI_LOGIN
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.account.save.ActiveAccount
import cz.anty.purkynka.account.ui.AccountEditActivity
import cz.anty.purkynka.attendance.AttendanceSearchFragment
import cz.anty.purkynka.dashboard.DashboardFragment
import cz.anty.purkynka.debug.DebugActivity
import cz.anty.purkynka.grades.GradesFragment
import cz.anty.purkynka.lunches.*
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.settings.SettingsActivity
import cz.anty.purkynka.utils.*
import cz.anty.purkynka.wifilogin.WifiLoginFragment
import eu.codetopic.java.utils.debug.DebugMode
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.to
import eu.codetopic.utils.*
import eu.codetopic.utils.broadcast.LocalBroadcast
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.ctx


class MainActivity : NavigationActivity() {

    companion object {
        private const val LOG_TAG = "MainActivity"

        private const val TAG_DIALOG_UNINSTALL_OLD_APP = "DIALOG_UNINSTALL_OLD_APP"

        private const val REQUEST_CODE_EDIT_ACCOUNT: Int = 1

        private const val DATA_SCHEME_FRAGMENT_SHORTCUT = "shortcut"

        private const val EXTRA_FRAGMENT_CLASS = "cz.anty.purkynka.$LOG_TAG.EXTRA_FRAGMENT_CLASS"
        private const val EXTRA_FRAGMENT_EXTRAS = "cz.anty.purkynka.$LOG_TAG.EXTRA_FRAGMENT_EXTRAS"

        private val optionsMenuMap = biMapOf<Int, Class<out Fragment>>(
                R.id.nav_dashboard to DashboardFragment::class.java,
                R.id.nav_grades to GradesFragment::class.java,
                R.id.nav_wifi_login to WifiLoginFragment::class.java,
                R.id.nav_attendance to AttendanceSearchFragment::class.java,
                R.id.nav_lunches to LunchesDecideFragment::class.java,
                R.id.nav_lunches_login to LunchesLoginFragment::class.java,
                R.id.nav_lunches_order to LunchesOrderFragment::class.java,
                R.id.nav_lunches_burza to LunchesBurzaFragment::class.java,
                R.id.nav_lunches_burza_watcher to LunchesBurzaWatcherFragment::class.java
        )

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

    private val invalidateReceiver: BroadcastReceiver =
            receiver { _, _ -> invalidateNavigationMenu() }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar) // TODO: Animated app splash screen
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) processIntent(intent)

        enableSwitchingAccounts = true
        enableActiveAccountEditButton = true

        setNavigationViewAppIconBitmap(run cropImage@ {
            val iconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_foreground)
            val iconWidth = iconBitmap.width
            val iconHeight = iconBitmap.height
            val cropWidth = iconWidth / 2
            val cropHeight = iconHeight / 2
            val cropStartX = (iconWidth - cropWidth) / 2
            val cropStartY = (iconHeight - cropHeight) / 2
            return@cropImage Bitmap.createBitmap(iconBitmap,
                    cropStartX, cropStartY, cropWidth, cropHeight)
        })

        findViewById<NavigationView>(R.id.navigationView).apply {
            val white = ContextCompat.getColorStateList(ctx, android.R.color.white)
            itemTextColor = white
            itemIconTintList = white
        }

        if (savedInstanceState == null && !DebugMode.isEnabled) {
            val appCtx = applicationContext
            val self = this.asReference()
            launch(UI) checkForOld@ {
                val isOldInstalled = bg {
                    appCtx.packageManager.isAppInstalled(APP_OLD_PACKAGE_NAME)
                }.await()

                if (!isOldInstalled) return@checkForOld

                self().apply {
                    UninstallOldAppDialogFragment().show(
                            supportFragmentManager,
                            TAG_DIALOG_UNINSTALL_OLD_APP
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun extractIntentSerializableTarget(intent: Intent): Pair<Class<out Fragment>, Bundle?>? =
            intent.getSerializableExtra(EXTRA_FRAGMENT_CLASS)
                    .to<Class<out Fragment>>()
                    ?.let {
                        it to intent.getBundleExtra(EXTRA_FRAGMENT_EXTRAS)
                    }

    private fun extractIntentDataTarget(intent: Intent): Pair<Class<out Fragment>, Bundle?>? =
            intent.data?.takeIf { it.scheme == DATA_SCHEME_FRAGMENT_SHORTCUT }
                    ?.let { data ->
                        data.host?.let { Class.forName(it).to<Class<out Fragment>>() }
                                ?.let {
                                    it to data.queryParameterNames
                                            .map { it to data.getQueryParameter(it) }
                                            .let { bundleOf(*it.toTypedArray()) }
                                }
                    }

    private fun processIntent(intent: Intent?) {
        if (intent == null) return

        val (clazz: Class<out Fragment>?, extras: Bundle?) =
                extractIntentSerializableTarget(intent)
                        ?: extractIntentDataTarget(intent)
                        ?: null to null

        clazz?.let { replaceFragment(clazz, extras) }
    }

    override fun onStart() {
        super.onStart()

        register()
    }

    override fun onStop() {
        unregister()

        super.onStop()
    }

    private fun register() {
        registerReceiver(
                invalidateReceiver,
                intentFilter(Accounts.ACTION_ACCOUNTS_CHANGED)
        )
        LocalBroadcast.registerReceiver(
                invalidateReceiver,
                intentFilter(
                        ActiveAccount.getter,
                        LunchesLoginData.getter
                )
        )

        invalidateNavigationMenu()
    }

    private fun unregister() {
        LocalBroadcast.unregisterReceiver(invalidateReceiver)
        unregisterReceiver(invalidateReceiver)
    }

    override fun onCreateAccountNavigationMenu(menu: Menu): Boolean {
        super.onCreateAccountNavigationMenu(menu)
        menuInflater.inflate(R.menu.activity_main_accounts, menu)
        val accounts = Accounts.getAll(this)
        val (_, activeAccount) = ActiveAccount.getWithId()
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
            R.id.menu_item_account -> run setAccount@ {
                val accountManager = accountManager
                val name = item.title.toString()
                val account = Accounts.getByNameOrNull(accountManager, name) ?: run {
                    Log.e(LOG_TAG, "onAccountNavigationItemSelected(item=$item)" +
                            " -> Failed to switch active account" +
                            " -> Account with name '$name' not found")
                    return@setAccount
                }
                val accountId = Accounts.getId(accountManager, account)
                ActiveAccount.set(accountId)
            }
            else -> return super.onAccountNavigationItemSelected(item)
        }
        return true
    }

    override fun onUpdateActiveAccountName(): CharSequence {
        return ActiveAccount.getWithId().second?.name ?: ""
    }

    override fun onEditAccountButtonClick(v: View): Boolean {
        startActivityForResult(
                Intent(this, AccountEditActivity::class.java)
                        .putExtra(AccountEditActivity.KEY_ACCOUNT,
                                ActiveAccount.getWithId().second ?: return false),
                REQUEST_CODE_EDIT_ACCOUNT)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_EDIT_ACCOUNT) {
            if (resultCode != Activity.RESULT_OK || data == null) return

            val accountManager = accountManager
            val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            val account = Accounts.getByNameOrNull(accountManager, accountName) ?: run {
                Log.e(LOG_TAG, "onActivityResult()" +
                        " -> Failed to switch active account" +
                        " -> Account with name '$accountName' not found")
                return
            }
            val accountId = Accounts.getId(accountManager, account)
            ActiveAccount.set(accountId)
            return
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
        menu.findItem(R.id.nav_attendance).icon =
                getIconics(ICON_ATTENDANCE).actionBar()

        val loggedInLunches = ActiveAccount.getWithId().first
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
        // Deselect all items
        optionsMenuMap.keys.forEach { menu.findItem(it).isChecked = false }

        val itemId = currentFragment?.javaClass?.let { optionsMenuMap.inverse[it] }
                ?: return super.onUpdateSelectedNavigationMenuItem(currentFragment, menu)

        menu.findItem(itemId).isChecked = true
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val fragmentClass = optionsMenuMap[item.itemId]
                ?: run {
                    when (item.itemId) {
                        R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                        R.id.nav_debug -> startActivity(Intent(this, DebugActivity::class.java))
                        R.id.nav_contact_facebook -> AndroidUtils.openUri(this, URL_FACEBOOK_PAGE, R.string.toast_browser_failed)
                        R.id.nav_contact_web_page -> AndroidUtils.openUri(this, URL_WEB_PAGE, R.string.toast_browser_failed)
                        R.id.nav_contact_web_page_donate -> AndroidUtils.openUri(this, URL_WEB_DONATE_PAGE, R.string.toast_browser_failed)
                        else -> return super.onNavigationItemSelected(item)
                    }
                    return true
                }

        replaceFragment(fragmentClass)
        return true
    }

    override fun onBeforeReplaceFragment(ft: FragmentTransaction, fragment: Fragment?) {
        ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
    }
}

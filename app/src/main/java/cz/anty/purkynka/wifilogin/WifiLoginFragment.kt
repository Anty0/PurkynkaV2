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

package cz.anty.purkynka.wifilogin

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.anty.purkynka.utils.ICON_WIFI_LOGIN
import cz.anty.purkynka.R
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.wifilogin.load.WifiLoginFetcher
import cz.anty.purkynka.wifilogin.load.WifiLoginFetcher.LoginResult.*
import cz.anty.purkynka.wifilogin.save.WifiData
import cz.anty.purkynka.wifilogin.save.WifiLoginData
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.receiver
import eu.codetopic.utils.getIconics
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.ui.activity.fragment.IconProvider
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import eu.codetopic.utils.ui.view.hideKeyboard
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.fragment_wifi_login.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx

/**
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class WifiLoginFragment : NavigationFragment(), TitleProvider, ThemeProvider, IconProvider {

    override val title: CharSequence
        get() = getText(R.string.title_fragment_wifi_login)
    override val themeId: Int
        get() = R.style.AppTheme_WifiLogin
    override val icon: Bitmap
        get() = ctx.getIconics(ICON_WIFI_LOGIN).sizeDp(48).toBitmap()

    private val accountHolder = ActiveAccountHolder(holder)

    private var accountPrimary: Boolean? = null

    private var userLoggedIn = false
    private var username = ""

    private var loginCount: Int? = null

    private val wifiLoginDataChangeReceiver = receiver { _, _ -> update() }

    private val wifiDataChangeReceiver = receiver { _, _ -> update() }

    init {
        val self = this.asReference()
        accountHolder.addChangeListener {
            self().update()?.join()
        }
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
                                     savedInstanceState: Bundle?): View? {
        val themedContext = ContextThemeWrapper(inflater.context, themeId)
        val themedInflater = inflater.cloneInContext(themedContext)
        return themedInflater.inflate(R.layout.fragment_wifi_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        butEnable.setOnClickListener(::onEnableClick)
        butDisable.setOnClickListener(::onDisableClick)
        butLogin.setOnClickListener(::onLoginClick)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        val self = this.asReference()
        val holder = holder
        launch(UI) {
            holder.showLoading()

            arrayOf(
                    self().update(),
                    self().accountHolder.update()
            ).forEach { it?.join() }

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }

    override fun onStart() {
        super.onStart()

        register()
        accountHolder.register()
    }

    override fun onStop() {
        accountHolder.unregister()
        unregister()

        super.onStop()
    }

    private fun register(): Job? {
        LocalBroadcast.registerReceiver(
                wifiLoginDataChangeReceiver,
                intentFilter(WifiLoginData.getter)
        )
        LocalBroadcast.registerReceiver(
                wifiDataChangeReceiver,
                intentFilter(WifiLoginData.getter)
        )

        return update()
    }

    private fun unregister() {
        LocalBroadcast.unregisterReceiver(wifiDataChangeReceiver)
        LocalBroadcast.unregisterReceiver(wifiLoginDataChangeReceiver)
    }

    private fun update(): Job? {
        view ?: return null
        val appContext = act.applicationContext
        val self = this.asReference()

        return launch(UI) {
            self().accountPrimary = bg {
                Accounts.getAll(appContext).firstOrNull()
            }.await() == accountHolder.account

            self().userLoggedIn = self().accountHolder.accountId?.let {
                bg { WifiLoginData.loginData.isLoggedIn(it) }.await()
            } ?: false
            self().username = self().accountHolder.accountId?.let {
                bg { WifiLoginData.loginData.getUsername(it) }.await()
            } ?: ""

            self().loginCount = self().accountHolder.accountId?.let {
                bg { WifiData.instance.getLoginCount(it) }.await()
            }

            self().updateUi()
        }
    }

    private fun updateUi() {
        view ?: return

        txtWarnUnsupportedDevice.visibility =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    View.VISIBLE
                else View.GONE

        if (userLoggedIn) {
            boxInPassword.visibility = View.GONE
            butEnable.visibility = View.GONE

            butDisable.visibility = View.VISIBLE
            txtWarnNonPrimaryAccount.visibility =
                    if (accountPrimary == false) View.VISIBLE else View.GONE
            if (accountPrimary == true) {
                txtLoginCounter.text = txtLoginCounter.context.getFormattedText(
                        R.string.text_view_login_counter,
                        loginCount ?: Double.NaN.toString()
                )
                txtLoginCounter.visibility = View.VISIBLE
            } else txtLoginCounter.visibility = View.GONE

            boxInUsername.isEnabled = false
            inUsername.setText(username)

            inPassword.setText("") // reset password input
        } else {
            boxInPassword.visibility = View.VISIBLE
            butEnable.visibility = View.VISIBLE

            butDisable.visibility = View.GONE
            txtWarnNonPrimaryAccount.visibility = View.GONE
            txtLoginCounter.visibility = View.GONE

            boxInUsername.isEnabled = true
            inUsername.takeIf { it.text.isEmpty() }?.setText(username)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onEnableClick(v: View) {
        val accountId = accountHolder.accountId ?:
                return longSnackbar(boxScrollView, R.string.snackbar_no_account_enable).show()
        val username = inUsername.text.toString()
        val password = inPassword.text.toString()
        val holder = holder

        launch(UI) {
            holder.showLoading()

            bg { WifiLoginData.loginData.login(accountId, username, password) }.await()

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onDisableClick(v: View) {
        val accountId = accountHolder.accountId ?:
                return longSnackbar(boxScrollView, R.string.snackbar_no_account_disable).show()
        val holder = holder

        launch(UI) {
            holder.showLoading()

            bg { WifiLoginData.loginData.logout(accountId) }.await()

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }

    private fun onLoginClick(v: View) {
        act.currentFocus?.hideKeyboard()

        val accountId = accountHolder.accountId ?:
                return longSnackbar(boxScrollView, R.string.snackbar_no_account_login).show()
        val loggedIn = userLoggedIn
        val username = inUsername.text.toString()
        val password = inPassword.text.toString()
        val holder = holder
        val boxScrollViewRef = boxScrollView.asReference()

        launch(UI) {
            holder.showLoading()

            val (fUsername, fPassword) = if (loggedIn)
                bg { WifiLoginData.loginData.getCredentials(accountId) }.await()
            else username to password

            if (fUsername == null || fPassword == null) {
                longSnackbar(boxScrollViewRef(), R.string.snackbar_failed_to_retrieve_login_data).show()
            } else {
                val result = WifiLoginFetcher.tryLoginGui(v.context,
                        fUsername, fPassword, WifiLoginFetcher.ForceLogin.ASK).await()
                when (result) {
                    SUCCESS -> longSnackbar(boxScrollViewRef(), R.string.snackbar_wifi_logging_success).show()
                    FAIL_CONNECTION -> longSnackbar(boxScrollViewRef(), R.string.snackbar_wifi_logging_fail).show()
                    FAIL_NO_LOGIN_PAGE -> longSnackbar(boxScrollViewRef(), R.string.snackbar_wifi_logging_fail_no_page).show()
                    else -> {} // ignored
                }
            }

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }
}
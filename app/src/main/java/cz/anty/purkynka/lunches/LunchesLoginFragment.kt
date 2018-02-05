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

package cz.anty.purkynka.lunches

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.lunches.save.LunchesLoginData
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.fragment_lunches_login.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.design.longSnackbar

/**
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class LunchesLoginFragment : NavigationFragment(), TitleProvider, ThemeProvider {

    companion object {

        private const val LOG_TAG = "LunchesLoginFragment"
    }

    override val title: CharSequence
        get() = getText(R.string.title_fragment_lunches_login)
    override val themeId: Int
        get() = R.style.AppTheme_Lunches

    private val accountHolder = ActiveAccountHolder(holder)

    private var userLoggedIn = false
    private var username = ""

    private val loginDataChangedReceiver = AndroidExtensions.broadcast { _, _ ->
        Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
        updateWithLoading()
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
                                     savedInstanceState: Bundle?): View? {
        val themedContext = ContextThemeWrapper(inflater.context, themeId)
        val themedInflater = inflater.cloneInContext(themedContext)
        return themedInflater.inflate(R.layout.fragment_lunches_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        butLogin.setOnClickListener { login() }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        launch(UI) {
            holder.showLoading()

            arrayOf(
                    update(),
                    accountHolder.update()
            ).forEach { it.join() }

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

    private fun register(): Job {
        LocalBroadcast.registerReceiver(
                receiver = loginDataChangedReceiver,
                filter = AndroidExtensions.intentFilter(LunchesLoginData.getter)
        )

        return update()
    }

    private fun unregister() {
        LocalBroadcast.unregisterReceiver(loginDataChangedReceiver)
    }

    private fun updateWithLoading(): Job {
        return launch(UI) {
            holder.showLoading()

            update().join()

            holder.hideLoading()
        }
    }

    private fun update(): Job = launch(UI) {
        userLoggedIn = accountHolder.accountId?.let {
            bg { LunchesLoginData.loginData.isLoggedIn(it) }.await()
        } ?: false
        username = accountHolder.accountId?.let {
            bg { LunchesLoginData.loginData.getUsername(it) }.await()
        } ?: ""

        updateUi()
    }

    private fun updateUi() {
        if (view == null) return

        if (userLoggedIn) {
            switchFragment(LunchesOrderFragment::class.java)
        } else {
            inUsername.takeIf { it.text.isEmpty() }?.setText(username)
        }
    }

    private fun login() {
        /*val account = accountHolder.account ?: run {
            longSnackbar(boxLogin, R.string.snackbar_no_account_login)
            return
        }*/
        val accountId = accountHolder.accountId ?: run {
            longSnackbar(boxLogin, R.string.snackbar_no_account_login)
            return
        }
        val username = inUsername.text.toString()
        val password = inPassword.text.toString()

        launch(UI) {
            holder.showLoading()

            bg { LunchesLoginData.loginData.login(accountId, username, password) }.await()

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }
}
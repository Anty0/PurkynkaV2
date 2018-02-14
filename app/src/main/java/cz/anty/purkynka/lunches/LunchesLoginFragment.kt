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

import android.accounts.Account
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.anty.purkynka.utils.Constants.ICON_LUNCHES
import cz.anty.purkynka.R
import cz.anty.purkynka.utils.Utils
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesData.SyncResult.*
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.lunches.sync.LunchesSyncAdapter
import eu.codetopic.java.utils.JavaExtensions.ifTrue
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.AndroidExtensions.getIconics
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.ui.activity.fragment.IconProvider
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
import org.jetbrains.anko.coroutines.experimental.Ref
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.ctx
import proguard.annotation.KeepName

/**
 * @author anty
 */
@KeepName
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class LunchesLoginFragment : NavigationFragment(), TitleProvider, ThemeProvider, IconProvider {

    companion object {

        private const val LOG_TAG = "LunchesLoginFragment"

        suspend fun doLogin(viewRef: Ref<View>,
                          account: Account, accountId: String,
                          username: String, password: String): Boolean {
            bg {
                LunchesData.instance.resetFirstSyncState(accountId)
                LunchesLoginData.loginData.login(accountId, username, password)
            }.await()

            // Sync will be triggered later by login change broadcast
            return if (Utils.awaitForSyncCompleted(account, LunchesSyncAdapter.CONTENT_AUTHORITY)) {
                val syncResult = bg { LunchesData.instance.getLastSyncResult(accountId) }.await()
                if (syncResult == FAIL_LOGIN) {
                    longSnackbar(viewRef(), R.string.snackbar_lunches_login_fail)
                    bg { LunchesLoginData.loginData.logout(accountId) }.await()

                    false
                } else true
            } else {
                longSnackbar(viewRef(), R.string.snackbar_sync_start_fail)
                bg { LunchesLoginData.loginData.logout(accountId) }.await()

                false
            }
        }

        suspend fun doLogout(accountId: String): Boolean {
            bg {
                LunchesLoginData.loginData.logout(accountId)
                LunchesData.instance.resetFirstSyncState(accountId)
            }.await()

            // TODO: after adding notifications to lunches, add their canceling here
            /*NotifyManager.requestCancelAll(
                    context = appContext,
                    groupId = AccountNotifyGroup.idFor(accountId),
                    channelId = ?
            )*/

            return true
        }
    }

    override val title: CharSequence
        get() = getText(R.string.title_fragment_lunches_login)
    override val themeId: Int
        get() = R.style.AppTheme_Lunches
    override val icon: Bitmap
        get() = ctx.getIconics(ICON_LUNCHES).sizeDp(48).toBitmap()

    private val accountHolder = ActiveAccountHolder(holder)

    private var userLoggedIn = false
    private var username = ""

    private val loginDataChangedReceiver = broadcast { _, _ ->
        Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
        updateWithLoading()
    }

    init {
        val self = this.asReference()
        accountHolder.addChangeListener {
            self().update().join()
            if (self().userLoggedIn) {
                // App was switched to logged in user
                // Let's switch fragment
                self().switchFragment(LunchesOrderFragment::class.java)
            }
        }
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

        val self = this.asReference()
        val holder = holder
        launch(UI) {
            holder.showLoading()

            arrayOf(
                    self().update(),
                    self().accountHolder.update()
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
        val self = this.asReference()
        val holder = holder
        return launch(UI) {
            holder.showLoading()

            self().update().join()

            holder.hideLoading()
        }
    }

    private fun update(): Job {
        val self = this.asReference()
        return launch(UI) {
            self().userLoggedIn = self().accountHolder.accountId?.let {
                bg { LunchesLoginData.loginData.isLoggedIn(it) }.await()
            } ?: false
            self().username = self().accountHolder.accountId?.let {
                bg { LunchesLoginData.loginData.getUsername(it) }.await()
            } ?: ""

            self().updateUi()
        }
    }

    private fun updateUi() {
        view ?: return

        if (userLoggedIn) {
        } else {
            inUsername.takeIf { it.text.isEmpty() }?.setText(username)
        }
    }

    private fun login() {
        val view = view ?: return

        val account = accountHolder.account ?: run {
            longSnackbar(boxLogin, R.string.snackbar_no_account_login)
            return
        }
        val accountId = accountHolder.accountId ?: run {
            longSnackbar(boxLogin, R.string.snackbar_no_account_login)
            return
        }
        val username = inUsername.text.toString()
        val password = inPassword.text.toString()

        val self = this.asReference()
        val viewRef = view.asReference()
        val holder = holder
        launch(UI) {
            holder.showLoading()

            doLogin(viewRef, account, accountId, username, password) ifTrue {
                // Success :D
                // Let's switch fragment
                self().switchFragment(LunchesOrderFragment::class.java)
            }

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }
}
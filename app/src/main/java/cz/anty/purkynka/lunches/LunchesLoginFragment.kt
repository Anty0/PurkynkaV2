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
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.analytics.FirebaseAnalytics
import cz.anty.purkynka.utils.ICON_LUNCHES
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.lunches.notify.LunchesChangesNotifyChannel
import cz.anty.purkynka.lunches.save.LunchesData.SyncResult.*
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.lunches.sync.LunchesSyncer
import cz.anty.purkynka.utils.FBA_LUNCHES_LOGIN
import eu.codetopic.java.utils.ifTrue
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.*
import eu.codetopic.utils.receiver
import eu.codetopic.utils.getIconics
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.ui.activity.fragment.IconProvider
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import eu.codetopic.utils.ui.view.hideKeyboard
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.fragment_lunches_login.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.io.IOException
import org.jetbrains.anko.coroutines.experimental.Ref
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.act
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

        suspend fun doLogin(appContext: Context, viewRef: Ref<View>,
                          account: Account, accountId: String,
                          username: String, password: String): Boolean {
            val syncResult = run sync@ {
                try {
                    bg {
                        LunchesSyncer.performSync(
                                context = appContext,
                                account = account,
                                credentials = username to password,
                                firstSync = true
                        )
                    }.await()
                    return@sync SUCCESS
                } catch (e: Exception) {
                    return@sync when (e) {
                        is WrongLoginDataException -> FAIL_LOGIN
                        is IOException -> FAIL_CONNECT
                        else -> FAIL_UNKNOWN
                    }
                }
            }

            return run process@ {
                when (syncResult) {
                    SUCCESS -> {
                        bg {
                            LunchesLoginData.loginData
                                    .login(accountId, username, password)
                        }.await()
                        return@process true
                    }
                    FAIL_LOGIN -> {
                        longSnackbar(viewRef(), R.string.snackbar_lunches_login_fail)
                        return@process false
                    }
                    FAIL_CONNECT, FAIL_UNKNOWN -> {
                        longSnackbar(viewRef(), R.string.snackbar_sync_start_fail)
                        return@process false
                    }
                }
            }
        }

        suspend fun doLogout(appContext: Context,
                             accountId: String): Boolean {
            bg { LunchesLoginData.loginData.logout(accountId) }.await()

            NotifyManager.requestCancelAll(
                    context = appContext,
                    groupId = AccountNotifyGroup.idFor(accountId),
                    channelId = LunchesChangesNotifyChannel.ID
            )

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

    private var firebaseAnalytics: FirebaseAnalytics? = null

    private var userLoggedIn = false
    private var username = ""

    private val loginDataChangedReceiver = receiver { _, _ ->
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(ctx)
    }

    override fun onDestroy() {
        firebaseAnalytics = null

        super.onDestroy()
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
                                     savedInstanceState: Bundle?): View? {
        val themedContext = ContextThemeWrapper(inflater.context, themeId)
        val themedInflater = inflater.cloneInContext(themedContext)
        return themedInflater.inflate(R.layout.fragment_lunches_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        butLogin.setOnClickListener {
            act.currentFocus?.hideKeyboard()
            login()
        }
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

    private fun register(): Job {
        LocalBroadcast.registerReceiver(
                receiver = loginDataChangedReceiver,
                filter = intentFilter(LunchesLoginData.getter)
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

        firebaseAnalytics?.logEvent(FBA_LUNCHES_LOGIN, null)

        val appContext = view.context.applicationContext
        val self = this.asReference()
        val viewRef = view.asReference()
        val holder = holder
        launch(UI) {
            holder.showLoading()

            doLogin(appContext, viewRef, account, accountId, username, password) ifTrue {
                // Success :D
                // Let's switch fragment
                self().switchFragment(LunchesOrderFragment::class.java)
            }

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }
}
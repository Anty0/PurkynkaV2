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

import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import cz.anty.purkynka.lunches.notify.LunchesChangesNotifyChannel
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesData.SyncResult.*
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.lunches.sync.LunchesSyncAdapter
import cz.anty.purkynka.lunches.ui.LunchOptionsGroupItem
import cz.anty.purkynka.lunches.ui.LunchesCreditItem
import cz.anty.purkynka.utils.FBA_LUNCHES_LOGOUT
import cz.anty.purkynka.utils.ICON_LUNCHES_ORDER
import cz.anty.purkynka.utils.awaitForSyncCompleted
import eu.codetopic.java.utils.ifFalse
import eu.codetopic.java.utils.ifTrue
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.edit
import eu.codetopic.utils.getIconics
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.receiver
import eu.codetopic.utils.ui.activity.fragment.IconProvider
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import eu.codetopic.utils.ui.container.adapter.CustomItemAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.recycler.Recycler
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.ctx
import proguard.annotation.KeepName

/**
 * @author anty
 */
@KeepName
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class LunchesOrderFragment : NavigationFragment(), TitleProvider, ThemeProvider, IconProvider {

    companion object {

        private const val LOG_TAG = "LunchesOrderFragment"
    }

    override val title: CharSequence
        get() = getText(R.string.title_fragment_lunches_order)
    override val themeId: Int
        get() = R.style.AppTheme_Lunches
    override val icon: Bitmap
        get() = ctx.getIconics(ICON_LUNCHES_ORDER).sizeDp(48).toBitmap()

    private val accountHolder = ActiveAccountHolder(holder)

    private val loginDataChangedReceiver = receiver { _, _ ->
        Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
        updateWithLoading()
    }
    private val dataChangedReceiver = receiver { _, _ ->
        Log.d(LOG_TAG, "dataChangedReceiver.onReceive()")
        update()
    }

    private var firebaseAnalytics: FirebaseAnalytics? = null

    private var userLoggedIn: Boolean = false
    private var credit: Float? = null
    private var lunchesList: List<LunchOptionsGroup>? = null
    private var isDataValid: Boolean = true
    private var lastSyncResult: LunchesData.SyncResult? = SUCCESS

    private var showingSyncResult: LunchesData.SyncResult? = SUCCESS
    private var syncStatusSnackbar: Snackbar? = null

    private var validityRefreshRequested: Boolean = false

    private var recyclerManager: Recycler.RecyclerManagerImpl? = null
    private var adapter: CustomItemAdapter<CustomItem>? = null

    init {
        setHasOptionsMenu(true)

        val self = this.asReference()
        accountHolder.addChangeListener {
            self().update().join()
            self().apply {
                if (!userLoggedIn && view != null) {
                    // App was switched to not logged in user
                    // Let's switch fragment
                    switchFragment(LunchesLoginFragment::class.java)
                }
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

        adapter = CustomItemAdapter(themedContext)

        val manager = Recycler.inflate().withSwipeToRefresh().withItemDivider()
                .on(themedInflater, container, false)
                .setEmptyImage(themedContext.getIconics(ICON_LUNCHES_ORDER).sizeDp(72))
                .setEmptyText(R.string.empty_view_text_no_lunches_to_order)
                .setSmallEmptyText(R.string.empty_view_text_small_no_lunches_to_order)
                .setAdapter(adapter)
                .setOnRefreshListener { -> requestSyncWithRecyclerRefreshing() }
        recyclerManager = manager
        return manager.baseView
    }

    override fun onDestroyView() {
        recyclerManager = null
        adapter = null

        super.onDestroyView()
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
        LocalBroadcast.registerReceiver(loginDataChangedReceiver,
                intentFilter(LunchesLoginData.getter))
        LocalBroadcast.registerReceiver(dataChangedReceiver,
                intentFilter(LunchesData.getter))

        return update()
    }

    private fun unregister() {
        LocalBroadcast.unregisterReceiver(dataChangedReceiver)
        LocalBroadcast.unregisterReceiver(loginDataChangedReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        if (!userLoggedIn) return

        inflater.inflate(R.menu.fragment_lunches_order, menu)

        menu.findItem(R.id.action_refresh).icon = activity
                ?.getIconics(GoogleMaterial.Icon.gmd_refresh)
                ?.actionBar()

        menu.findItem(R.id.action_log_out).icon = activity
                ?.getIconics(CommunityMaterial.Icon.cmd_logout)
                ?.actionBar()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> requestSyncWithLoading()
            R.id.action_log_out -> logout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
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
            self().isDataValid = self().accountHolder.accountId?.let {
                bg { LunchesData.instance.isDataValid(it) }.await()
            } ?: true
            self().credit = self().accountHolder.accountId?.let {
                bg { LunchesData.instance.getCredit(it) }.await()
            }
            self().lunchesList = self().accountHolder.accountId?.let {
                bg { LunchesData.instance.getLunches(it) }.await()
            }
            self().lastSyncResult =
                    if (!self().userLoggedIn) SUCCESS // don't show sync result, if user is not logged in
                    else self().accountHolder.accountId?.let {
                        bg { LunchesData.instance.getLastSyncResult(it) }.await()
                    }
            self().accountHolder.accountId?.let {
                NotifyManager.sCancelAll(
                        context = self().takeIf { it.view != null }?.ctx ?: return@let,
                        groupId = AccountNotifyGroup.idFor(it),
                        channelId = LunchesChangesNotifyChannel.ID)
            }

            self().updateUi()
        }
    }

    private fun updateUi() {
        view ?: return

        adapter?.edit {
            clear()

            if (userLoggedIn && isDataValid) {
                accountHolder.accountId?.also { accountId ->
                    lunchesList
                            ?.takeIf { it.isNotEmpty() }
                            ?.sortedBy { it.date }
                            ?.also {
                                // add credit only if lunchesList is not null
                                credit?.also {
                                    add(LunchesCreditItem(it))
                                }
                            }
                            ?.map { LunchOptionsGroupItem(accountId, it) }
                            // TODO: maybe add week dividers
                            ?.let { addAll(it) }
                }
            }

            notifyAllItemsChanged()
        }

        // Allow menu visibility changes based on userLoggedIn state
        act.invalidateOptionsMenu()

        updateUiSyncResult()

        if (!isDataValid) {
            if (!validityRefreshRequested) {
                validityRefreshRequested = true
                requestSyncWithLoading()
            }
        } else {
            validityRefreshRequested = false
        }
    }

    private fun updateUiSyncResult(force: Boolean = false) {
        val view = view ?: return

        if (!force && showingSyncResult == lastSyncResult &&
                syncStatusSnackbar?.isShownOrQueued != false) return

        val self = this.asReference()
        when (lastSyncResult) {
            FAIL_UNKNOWN -> syncStatusSnackbar = indefiniteSnackbar(
                    view,
                    R.string.snackbar_lunches_refresh_fail_unknown,
                    R.string.snackbar_action_lunches_retry,
                    {
                        launch(UI) {
                            self().requestSyncWithLoading()?.join()
                            self().updateUiSyncResult(true)
                        }
                    }
            )
            FAIL_CONNECT -> syncStatusSnackbar = indefiniteSnackbar(
                    view,
                    R.string.snackbar_lunches_refresh_fail_connect,
                    R.string.snackbar_action_lunches_retry,
                    {
                        launch(UI) {
                            self().requestSyncWithLoading()?.join()
                            self().updateUiSyncResult(true)
                        }
                    }
            )
            FAIL_LOGIN -> syncStatusSnackbar = indefiniteSnackbar(
                    view,
                    R.string.snackbar_lunches_refresh_fail_login,
                    R.string.snackbar_action_lunches_logout,
                    {
                        launch(UI) {
                            self().logout()?.join()
                            self().updateUiSyncResult(true)
                        }
                    }
            )
            null -> syncStatusSnackbar = indefiniteSnackbar(
                    view,
                    R.string.snackbar_lunches_fail_no_account
            )
            else -> {
                syncStatusSnackbar?.apply {
                    dismiss()
                    syncStatusSnackbar = null
                }
            }
        }

        showingSyncResult = lastSyncResult
    }

    private fun requestSyncWithLoading(): Job? {
        if (!userLoggedIn) return null
        val view = view ?: return null

        val account = accountHolder.account ?: run {
            longSnackbar(view, R.string.snackbar_no_account_sync)
            return null
        }

        val viewRef = view.asReference()
        val holder = holder

        return launch(UI) {
            holder.showLoading()

            LunchesSyncAdapter.requestSync(account)

            awaitForSyncCompleted(account, LunchesSyncAdapter.CONTENT_AUTHORITY) ifFalse {
                longSnackbar(viewRef(), R.string.snackbar_sync_start_fail)
            }

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }

    private fun requestSyncWithRecyclerRefreshing(): Job? {
        if (!userLoggedIn) return null
        val view = view ?: return null

        val account = accountHolder.account ?: run {
            longSnackbar(view, R.string.snackbar_no_account_sync)
            return null
        }

        val viewRef = view.asReference()
        val recyclerManagerRef = recyclerManager?.asReference() ?: return null

        return launch(UI) {
            LunchesSyncAdapter.requestSync(account)

            awaitForSyncCompleted(account, LunchesSyncAdapter.CONTENT_AUTHORITY) ifFalse {
                longSnackbar(viewRef(), R.string.snackbar_sync_start_fail)
            }

            recyclerManagerRef().setRefreshing(false)
        }
    }

    private fun logout(): Job? {
        val view = view ?: return null

        if (!userLoggedIn) {
            switchFragment(LunchesLoginFragment::class.java)
            return null
        }

        val accountId = accountHolder.accountId ?: run {
            longSnackbar(view, R.string.snackbar_no_account_logout)
            return null
        }

        firebaseAnalytics?.logEvent(FBA_LUNCHES_LOGOUT, null)

        val self = this.asReference()
        val holder = holder
        val appContext = view.context.applicationContext
        return launch(UI) {
            holder.showLoading()

            LunchesLoginFragment.doLogout(appContext, accountId) ifTrue {
                // Success :D
                // Let's switch fragment
                self().takeIf { it.view != null }
                        ?.switchFragment(LunchesLoginFragment::class.java)
            }

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }
}
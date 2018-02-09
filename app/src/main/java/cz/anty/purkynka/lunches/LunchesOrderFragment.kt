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

import android.content.Context
import android.os.Bundle
import android.view.*
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import cz.anty.purkynka.Constants.ICON_LUNCHES
import cz.anty.purkynka.R
import cz.anty.purkynka.Utils
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.lunches.sync.LunchesSyncAdapter
import cz.anty.purkynka.lunches.ui.LunchOptionsGroupItem
import eu.codetopic.java.utils.JavaExtensions.ifFalse
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions
import eu.codetopic.utils.AndroidExtensions.edit
import eu.codetopic.utils.AndroidExtensions.getIconics
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.broadcast.LocalBroadcast
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
import org.jetbrains.anko.design.longSnackbar

/**
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class LunchesOrderFragment : NavigationFragment(), TitleProvider, ThemeProvider {

    companion object {

        private const val LOG_TAG = "LunchesOrderFragment"
    }

    override val title: CharSequence
        get() = getText(R.string.title_fragment_lunches_order)
    override val themeId: Int
        get() = R.style.AppTheme_Lunches

    private val accountHolder = ActiveAccountHolder(holder)

    private val loginDataChangedReceiver = AndroidExtensions.broadcast { _, _ ->
        Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
        updateWithLoading()
    }
    private val dataChangedReceiver = AndroidExtensions.broadcast { _, _ ->
        Log.d(LOG_TAG, "dataChangedReceiver.onReceive()")
        update()
    }

    private var userLoggedIn = false
    private var lunchesList: List<LunchOptionsGroup>? = null

    private var recyclerManager: Recycler.RecyclerManagerImpl? = null
    private var adapter: CustomItemAdapter<CustomItem>? = null

    init {
        setHasOptionsMenu(true)
        accountHolder.addChangeListener { update().join() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        adapter = CustomItemAdapter(context)
    }

    override fun onDetach() {
        adapter = null

        super.onDetach()
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
                                     savedInstanceState: Bundle?): View? {
        val themedContext = ContextThemeWrapper(inflater.context, themeId)
        val themedInflater = inflater.cloneInContext(themedContext)

        val manager = Recycler.inflate().withSwipeToRefresh().withItemDivider()
                .on(themedInflater, container, false)
                .setEmptyImage(themedContext.getIconics(ICON_LUNCHES).sizeDp(72))
                .setEmptyText(R.string.empty_view_text_no_lunches_to_order)
                .setSmallEmptyText(R.string.empty_view_text_small_no_lunches_to_order)
                .setAdapter(adapter)
                .setOnRefreshListener(::requestSyncWithRecyclerRefreshing)
        recyclerManager = manager
        return manager.baseView
    }

    override fun onDestroyView() {
        recyclerManager = null

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
            self().lunchesList = self().accountHolder.accountId?.let {
                bg { LunchesData.instance.getLunches(it) }.await()
            }

            self().updateUi()
        }
    }

    private fun updateUi() {
        view ?: return

        adapter?.edit {
            clear()
            if (userLoggedIn) {
                lunchesList
                        ?.sortedBy { it.date }
                        ?.map { LunchOptionsGroupItem(it) } // TODO: maybe add week dividers
                        ?.let { addAll(it) }
            }
        }

        // Allow menu visibility changes based on userLoggedIn state
        activity?.invalidateOptionsMenu()
    }

    private fun requestSyncWithLoading() {
        if (!userLoggedIn) return
        val view = view ?: return

        val account = accountHolder.account ?: run {
            longSnackbar(view, R.string.snackbar_no_account_sync)
            return
        }

        val viewRef = view.asReference()
        val holder = holder

        launch(UI) {
            holder.showLoading()

            LunchesSyncAdapter.requestSync(account)

            Utils.awaitForSyncCompleted(account, LunchesSyncAdapter.CONTENT_AUTHORITY) ifFalse {
                longSnackbar(viewRef(), R.string.snackbar_sync_start_fail)
            }

            //delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }

    private fun requestSyncWithRecyclerRefreshing() {
        if (!userLoggedIn) return
        val view = view ?: return

        val account = accountHolder.account ?: run {
            longSnackbar(view, R.string.snackbar_no_account_sync)
            return
        }

        val viewRef = view.asReference()
        val recyclerManagerRef = recyclerManager?.asReference() ?: return

        launch(UI) {
            LunchesSyncAdapter.requestSync(account)

            Utils.awaitForSyncCompleted(account, LunchesSyncAdapter.CONTENT_AUTHORITY) ifFalse {
                longSnackbar(viewRef(), R.string.snackbar_sync_start_fail)
            }

            recyclerManagerRef().setRefreshing(false)
        }
    }

    private fun logout() {
        if (!userLoggedIn) {
            switchFragment(LunchesLoginFragment::class.java)
            return
        }
        val view = view ?: return

        val accountId = accountHolder.accountId ?: run {
            longSnackbar(view, R.string.snackbar_no_account_logout)
            return
        }

        val self = this.asReference()
        val holder = holder
        launch(UI) {
            holder.showLoading()

            bg { LunchesLoginData.loginData.logout(accountId) }.await()

            self().switchFragment(LunchesLoginFragment::class.java)
            //delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }
}
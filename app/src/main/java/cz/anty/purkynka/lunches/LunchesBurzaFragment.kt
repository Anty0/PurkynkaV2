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
import android.view.*
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import cz.anty.purkynka.utils.ICON_LUNCHES_BURZA
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.lunches.data.LunchBurza
import cz.anty.purkynka.lunches.load.LunchesFetcher
import cz.anty.purkynka.lunches.load.LunchesParser
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.lunches.ui.LunchBurzaItem
import cz.anty.purkynka.lunches.ui.LunchesCreditItem
import eu.codetopic.java.utils.ifTrue
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.*
import eu.codetopic.utils.edit
import eu.codetopic.utils.getIconics
import eu.codetopic.utils.broadcast.LocalBroadcast
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
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.ctx
import proguard.annotation.KeepName
import java.io.IOException

/**
 * @author anty
 */
@KeepName
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class LunchesBurzaFragment : NavigationFragment(), TitleProvider, ThemeProvider, IconProvider {

    companion object {

        private const val LOG_TAG = "LunchesBurzaFragment"
    }

    override val title: CharSequence
        get() = getText(R.string.title_fragment_lunches_burza)
    override val themeId: Int
        get() = R.style.AppTheme_Lunches
    override val icon: Bitmap
        get() = ctx.getIconics(ICON_LUNCHES_BURZA).sizeDp(48).toBitmap()

    private val accountHolder = ActiveAccountHolder(holder)

    private val loginDataChangedReceiver = receiver { _, _ ->
        Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
        updateWithLoading(updateBurza = true)
    }
    private val dataChangedReceiver = receiver { _, _ ->
        Log.d(LOG_TAG, "dataChangedReceiver.onReceive()")
        update()
    }

    private var userLoggedIn: Boolean = false
    private var credit: Float? = null
    private var burzaList: List<LunchBurza>? = null

    private var recyclerManager: Recycler.RecyclerManagerImpl? = null
    private var adapter: CustomItemAdapter<CustomItem>? = null

    init {
        setHasOptionsMenu(true)

        val self = this.asReference()
        accountHolder.addChangeListener {
            self().update()?.join()
            if (!self().userLoggedIn) {
                // App was switched to not logged in user
                // Let's switch fragment
                self().switchFragment(LunchesLoginFragment::class.java)
            }
        }
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
                                     savedInstanceState: Bundle?): View? {
        val themedContext = ContextThemeWrapper(inflater.context, themeId)
        val themedInflater = inflater.cloneInContext(themedContext)

        adapter = CustomItemAdapter(themedContext)

        val manager = Recycler.inflate().withSwipeToRefresh().withItemDivider()
                .on(themedInflater, container, false)
                .setEmptyImage(themedContext.getIconics(ICON_LUNCHES_BURZA).sizeDp(72))
                .setEmptyText(R.string.empty_view_text_no_burza_lunches)
                .setSmallEmptyText(R.string.empty_view_text_small_no_burza_lunches)
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
                    self().update(updateBurza = true),
                    self().accountHolder.update()
            ).forEach { it?.join() }

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

        inflater.inflate(R.menu.fragment_lunches_burza, menu)

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

    private fun updateWithLoading(updateBurza: Boolean = false): Job {
        val self = this.asReference()
        val holder = holder
        return launch(UI) {
            holder.showLoading()

            self().update(updateBurza)?.join()

            holder.hideLoading()
        }
    }

    private fun update(updateBurza: Boolean = false): Job? {
        val view = view ?: return null

        val self = this.asReference()
        val viewRef = view.asReference()
        return launch(UI) {
            self().userLoggedIn = self().accountHolder.accountId?.let {
                bg { LunchesLoginData.loginData.isLoggedIn(it) }.await()
            } ?: false
            self().credit = self().accountHolder.accountId?.let {
                bg { LunchesData.instance.getCredit(it) }.await()
            }
            if (updateBurza) {
                self().burzaList = self().accountHolder.accountId?.let { accountId ->
                    bg {
                        try {
                            val loginData = LunchesLoginData.loginData

                            if (!loginData.isLoggedIn(accountId))
                                throw IllegalStateException("User is not logged in")

                            val (username, password) = loginData.getCredentials(accountId)

                            if (username == null || password == null)
                                throw IllegalStateException("Username or password is null")

                            val cookies = LunchesFetcher.login(username, password)

                            if (!LunchesFetcher.isLoggedIn(cookies))
                                throw WrongLoginDataException("Failed to login user with provided credentials")

                            val burzaLunchesHtml = LunchesFetcher.getLunchesBurzaElements(cookies)
                            val nBurzaLunches = LunchesParser.parseLunchesBurza(burzaLunchesHtml)

                            LunchesFetcher.logout(cookies)

                            return@bg nBurzaLunches
                        } catch (e: Exception) {
                            Log.w(LOG_TAG, "update() -> Failed to fetch burza lunches", e)

                            launch(UI) {
                                longSnackbar(viewRef(), when (e) {
                                    is WrongLoginDataException -> R.string.snackbar_lunches_fetch_burza_fail_login
                                    is IOException -> R.string.snackbar_lunches_fetch_burza_fail_connect
                                    else -> R.string.snackbar_lunches_fetch_burza_fail_unknown
                                })
                            }
                            return@bg null
                        }
                    }.await()
                }
            }

            self().updateUi()
        }
    }

    private fun updateUi() {
        view ?: return

        adapter?.edit {
            clear()

            if (userLoggedIn) {
                accountHolder.accountId?.also { accountId ->
                    burzaList
                            ?.takeIf { it.isNotEmpty() }
                            ?.map { LunchBurzaItem(accountId, it) }
                            ?.also {
                                // add credit only if burzaList is not null
                                credit?.also {
                                    add(LunchesCreditItem(it))
                                }
                            }
                            ?.let { addAll(it) }
                }
            }

            notifyAllItemsChanged()
        }

        // Allow menu visibility changes based on userLoggedIn state
        activity?.invalidateOptionsMenu()
    }

    private fun requestSyncWithLoading(): Job? {
        if (!userLoggedIn) return null
        view ?: return null

        val holder = holder
        return launch(UI) {
            holder.showLoading()

            update(updateBurza = true)?.join()

            //delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }

    private fun requestSyncWithRecyclerRefreshing(): Job? {
        if (!userLoggedIn) return null
        view ?: return null

        val recyclerManagerRef = recyclerManager?.asReference() ?: return null
        return launch(UI) {
            update(updateBurza = true)?.join()

            recyclerManagerRef().setRefreshing(false)
        }
    }

    private fun logout(): Job? {
        if (!userLoggedIn) {
            switchFragment(LunchesLoginFragment::class.java)
            return null
        }
        val view = view ?: return null

        val accountId = accountHolder.accountId ?: run {
            longSnackbar(view, R.string.snackbar_no_account_logout)
            return null
        }

        val self = this.asReference()
        val holder = holder
        val appContext = view.context.applicationContext
        return launch(UI) {
            holder.showLoading()

            LunchesLoginFragment.doLogout(appContext, accountId) ifTrue {
                // Success :D
                // Let's switch fragment
                self().switchFragment(LunchesLoginFragment::class.java)
            }

            //delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }
}
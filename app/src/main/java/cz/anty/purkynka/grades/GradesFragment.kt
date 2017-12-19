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

package cz.anty.purkynka.grades

import android.accounts.Account
import android.content.ContentResolver
import android.content.Context
import android.content.SyncStatusObserver
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import cz.anty.purkynka.R
import cz.anty.purkynka.accounts.ActiveAccountManager
import cz.anty.purkynka.accounts.notify.AccountNotificationChannel
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.load.GradesParser.toSubjects
import cz.anty.purkynka.grades.notify.GradesChangesNotificationGroup
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesData.SyncResult.*
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesMap
import cz.anty.purkynka.grades.save.GradesUiData
import cz.anty.purkynka.grades.sync.GradesSyncAdapter
import cz.anty.purkynka.grades.ui.GradeItem
import cz.anty.purkynka.grades.ui.SubjectItem
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.AndroidExtensions.edit
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.notifications.manager.NotificationsManager
import eu.codetopic.utils.notifications.manager.data.NotificationId
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import eu.codetopic.utils.ui.container.adapter.CustomItemAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.recycler.Recycler
import eu.codetopic.utils.ui.view.holder.loading.LoadingVH
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*
import kotlinx.android.synthetic.main.fragment_grades.*
import kotlinx.android.synthetic.main.fragment_grades.view.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.longSnackbar
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class GradesFragment : NavigationFragment(), TitleProvider, ThemeProvider {

    companion object {

        private const val LOG_TAG = "GradesFragment"

        private const val SAVE_EXTRA_SORT = "$LOG_TAG.SORT"
        private const val SAVE_EXTRA_SEMESTER = "$LOG_TAG.SEMESTER"

        suspend fun aWaitForSyncStart(account: Account) = aWaitForSyncState {
            (ContentResolver.isSyncActive(account, GradesSyncAdapter.CONTENT_AUTHORITY) ||
                    ContentResolver.isSyncPending(account, GradesSyncAdapter.CONTENT_AUTHORITY))
                    .also { Log.d(LOG_TAG, "aWaitForSyncStart(account=$account) -> $it") }
        }

        suspend fun aWaitForSyncEnd(account: Account) = aWaitForSyncState {
            (!ContentResolver.isSyncActive(account, GradesSyncAdapter.CONTENT_AUTHORITY) &&
                    !ContentResolver.isSyncPending(account, GradesSyncAdapter.CONTENT_AUTHORITY))
                    .also { Log.d(LOG_TAG, "aWaitForSyncEnd(account=$account) -> $it") }
        }

        inline suspend fun aWaitForSyncState(crossinline condition: () -> Boolean) = suspendCoroutine<Unit> { cont ->
            if (run(condition)) {
                cont.resume(Unit)
                return@suspendCoroutine
            }

            var observerHandle: Any? = null
            val observer = SyncStatusObserver {
                if (run(condition)) {
                    observerHandle?.let {
                        observerHandle = null
                        ContentResolver.removeStatusChangeListener(it)
                        cont.resume(Unit)
                    }
                }
            }
            val mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING or ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
            observerHandle = ContentResolver.addStatusChangeListener(mask, observer)

            if (run(condition)) {
                observerHandle?.let {
                    observerHandle = null
                    ContentResolver.removeStatusChangeListener(it)

                    cont.resume(Unit)
                    return@suspendCoroutine
                }
            }
        }

        /*fun aWaitForSync(account: Account) = async(UI) {
            val condition = {
                !ContentResolver.isSyncActive(account, GradesSyncAdapter.CONTENT_AUTHORITY) &&
                        !ContentResolver.isSyncPending(account, GradesSyncAdapter.CONTENT_AUTHORITY)
            }

            Log.d(LOG_TAG, "aWaitForSync(account=$account) -> condition() -> 1 -> ${run(condition)}")
            if (run(condition)) return@async Unit

            var observerHandle: Any? = null
            val observer = SyncStatusObserver {
                Log.d(LOG_TAG, "aWaitForSync(account=$account) -> condition() -> 3 -> ${run(condition)}")
                if (run(condition)) {
                    observerHandle?.let {
                        observerHandle = null
                        ContentResolver.removeStatusChangeListener(it)

                        @Suppress("UNCHECKED_CAST")
                        (this@async as Continuation<Unit>).resume(Unit)
                    }
                }
            }
            val mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING or ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
            observerHandle = ContentResolver.addStatusChangeListener(mask, observer)

            Log.d(LOG_TAG, "aWaitForSync(account=$account) -> condition() -> 2 -> ${run(condition)}")
            if (run(condition)) {
                observerHandle?.let {
                    observerHandle = null
                    ContentResolver.removeStatusChangeListener(it)

                    return@async Unit
                }
            }

            return@async COROUTINE_SUSPENDED
        }*/
    }

    override val title: CharSequence
        get() = getText(R.string.action_show_grades)
    override val themeId: Int
        get() = R.style.AppTheme_Grades

    private val accountHolder = ActiveAccountHolder()
    private val layLogin = LayoutLogin(accountHolder, holder)
    private val layGrades = LayoutGrades(accountHolder, holder)
    private val laySyncStatus = LayoutSyncStatus(accountHolder, layLogin, layGrades)

    init { setHasOptionsMenu(true) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layGrades.restore(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        layGrades.save(outState)
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
                                     savedInstanceState: Bundle?): View? {
        val themedContext = ContextThemeWrapper(inflater.context, R.style.AppTheme_Grades)
        val themedInflater = inflater.cloneInContext(themedContext)
        val view = themedInflater.inflate(R.layout.fragment_grades, container, false)

        layGrades.bindView(themedContext, themedInflater, view)
        layLogin.bindView(view)
        laySyncStatus.bindView(view)

        return view
    }

    override fun onResume() {
        super.onResume()

        launch(UI) {
            holder.showLoading()

            // TODO: Create superclass for these layouts and call onEach register
            arrayOf(
                    layLogin.register(),
                    layGrades.register(),
                    laySyncStatus.register(),
                    accountHolder.register()
            ).forEach { it.join() }

            holder.hideLoading()
        }
    }

    override fun onPause() {
        accountHolder.unregister()
        laySyncStatus.unregister()
        layGrades.unregister()
        layLogin.unregister()

        super.onPause()
    }

    override fun onDestroyView() {
        laySyncStatus.unbindView()
        layLogin.unbindView()
        layGrades.unbindView()
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_grades_menu, menu) // FIXME: hide options menu on login screen
        // TODO: here only inflate menu, all other logic should be in layouts

        menu.findItem(
                if (layGrades.sort == GradesUiData.Sort.DATE) R.id.action_sort_date
                else R.id.action_sort_subject
        ).isChecked = true

        menu.findItem(
                if (layGrades.semester == Semester.FIRST) R.id.action_semester_first
                else R.id.action_semester_second
        ).isChecked = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item)) return true
        // TODO: all logic should be in layouts

        when (item.itemId) {
            R.id.action_sort_date -> {
                item.isChecked = true
                layGrades.sort = GradesUiData.Sort.DATE
            }
            R.id.action_sort_subject -> {
                item.isChecked = true
                layGrades.sort = GradesUiData.Sort.SUBJECTS
            }
            R.id.action_semester_first -> {
                item.isChecked = true
                layGrades.semester = Semester.FIRST
            }
            R.id.action_semester_second -> {
                item.isChecked = true
                layGrades.semester = Semester.SECOND
            }
            R.id.action_refresh -> layGrades.requestSyncWithLoading()
            R.id.action_log_out -> layLogin.logout()
            else -> return false
        }
        return true
    }

    private class ActiveAccountHolder {

        companion object {
            private const val LOG_TAG = "${GradesFragment.LOG_TAG}\$ActiveAccountHolder"

        }

        private val accountChangedReceiver = broadcast { _, _ ->
            Log.d(LOG_TAG, "accountChangedReceiver.onReceive()")
            updateData()
        }

        private val listeners = mutableListOf<() -> Unit>()

        var account: Account? = null
            private set
        var accountId: String? = null
            private set

        private fun updateData(): Job {
            val self = this.asReference()

            return launch(UI) {
                val accountWithId = bg { ActiveAccountManager.instance.activeAccountWithId }.await()
                val nAccount = accountWithId.first
                val nAccountId = accountWithId.second

                self().apply {
                    account = nAccount
                    accountId = nAccountId

                    listeners.forEach { it() }
                }
            }
        }

        fun addChangeListener(listener: () -> Unit) {
            listeners.add(listener)
        }

        fun register(): Job {
            LocalBroadcast.registerReceiver(accountChangedReceiver,
                    intentFilter(ActiveAccountManager.getter))

            return updateData()
        }

        fun unregister() {
            LocalBroadcast.unregisterReceiver(accountChangedReceiver)
        }
    }

    private class LayoutLogin(private val accountHolder: ActiveAccountHolder,
                              private val holder: LoadingVH) : LayoutContainer {

        companion object {
            private const val LOG_TAG = "${GradesFragment.LOG_TAG}\$LayoutLogin"

        }

        override var containerView: View? = null

        private val loginDataChangedReceiver = broadcast { _, _ ->
            Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
            updateData()
        }

        private var userLoggedIn = false
        private var username = ""

        init {
            accountHolder.addChangeListener { updateData() }
        }

        fun bindView(view: View) {
            containerView = view.boxLogin
            butLogin.setOnClickListener { login() }
        }

        fun unbindView() {
            containerView = null
            clearFindViewByIdCache()
        }

        fun register(): Job {
            LocalBroadcast.registerReceiver(loginDataChangedReceiver,
                    intentFilter(GradesLoginData.getter))

            return updateData()
        }

        fun unregister() {
            LocalBroadcast.unregisterReceiver(loginDataChangedReceiver)
        }

        private fun updateData(): Job = launch(UI) {
            userLoggedIn = accountHolder.accountId?.let {
                bg { GradesLoginData.loginData.isLoggedIn(it) }.await()
            } ?: false
            username = accountHolder.accountId?.let {
                bg { GradesLoginData.loginData.getUsername(it) }.await()
            } ?: ""

            update()
        }

        fun update() {
            if (containerView == null) return

            boxLogin.visibility = if (!userLoggedIn) {
                inUsername.takeIf { it.text.isEmpty() }?.setText(username)

                View.VISIBLE
            } else View.GONE
        }

        fun login() {
            val account = accountHolder.account ?:
                    return longSnackbar(boxLogin, R.string.snackbar_no_account_login).show()
            val accountId = accountHolder.accountId ?:
                    return longSnackbar(boxLogin, R.string.snackbar_no_account_login).show()
            val username = inUsername.text.toString()
            val password = inPassword.text.toString()

            launch(UI) {
                holder.showLoading()

                bg { GradesLoginData.loginData.login(accountId, username, password) }.await()

                // Sync will be triggered later by change broadcast, so we must wait for sync start before we can wait for sync end
                aWaitForSyncStart(account)
                aWaitForSyncEnd(account)

                delay(500) // Wait few loops to make sure, that content was updated.
                holder.hideLoading()
            }
        }

        fun logout() {
            val account = accountHolder.account ?:
                    return longSnackbar(boxLogin, R.string.snackbar_no_account_logout).show()
            val accountId = accountHolder.accountId ?:
                    return longSnackbar(boxLogin, R.string.snackbar_no_account_logout).show()

            launch(UI) {
                holder.showLoading()

                bg { GradesLoginData.loginData.logout(accountId) }.await()

                // Sync will be triggered later by change broadcast, so we must wait for sync start before we can wait for sync end
                aWaitForSyncStart(account)
                aWaitForSyncEnd(account)

                delay(500) // Wait few loops to make sure, that content was updated.
                holder.hideLoading()
            }
        }
    }

    private class LayoutGrades(private val accountHolder: ActiveAccountHolder,
                               private val holder: LoadingVH) : LayoutContainer {

        companion object {
            private const val LOG_TAG = "${GradesFragment.LOG_TAG}\$LayoutGrades"

        }

        override var containerView: View? = null

        private val loginDataChangedReceiver = broadcast { _, _ ->
            Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
            updateData()
        }
        private val dataChangedReceiver = broadcast { _, _ ->
            Log.d(LOG_TAG, "dataChangedReceiver.onReceive()")
            updateData()
        }

        private var userLoggedIn = false
        private var gradesMap: GradesMap? = null
        private val gradesChangesMap: MutableMap<NotificationId, Bundle> = mutableMapOf() // TODO: preserve screen rotation

        private var recyclerManager: Recycler.RecyclerManagerImpl? = null
        private var adapter: CustomItemAdapter<CustomItem>? = null

        var sort: GradesUiData.Sort = GradesUiData.instance.lastSort
            set(value) {
                field = value
                GradesUiData.instance.lastSort = value
                update()
            }
        var semester: Semester = Semester.AUTO.stableSemester
            set(value) {
                field = value
                update()
            }

        init {
            accountHolder.addChangeListener { updateData() }
        }

        fun restore(savedInstanceState: Bundle?) {
            savedInstanceState?.takeIf { it.containsKey(SAVE_EXTRA_SORT) }?.let {
                sort = try {
                    savedInstanceState.getSerializable(SAVE_EXTRA_SORT) as GradesUiData.Sort
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "onCreate() -> restoreSortState()", e)
                    GradesUiData.Sort.DATE
                }
            }
            savedInstanceState?.takeIf { it.containsKey(SAVE_EXTRA_SEMESTER) }?.let {
                semester = try {
                    savedInstanceState.getSerializable(SAVE_EXTRA_SEMESTER) as Semester
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "onCreate() -> restoreSemesterState()", e)
                    Semester.AUTO.stableSemester
                }
            }
        }

        fun save(outState: Bundle) {
            outState.putSerializable(SAVE_EXTRA_SORT, sort)
            outState.putSerializable(SAVE_EXTRA_SEMESTER, semester)
        }

        fun bindView(context: Context, inflater: LayoutInflater, view: View) {
            containerView = view.boxRecycler

            adapter = CustomItemAdapter(context)

            recyclerManager = Recycler.inflate().withSwipeToRefresh()
                    .on(inflater, boxRecycler, true)
                    .setEmptyImage(R.mipmap.ic_launcher_grades) // TODO: better image
                    .setEmptyText(R.string.empty_view_text_no_grades)
                    .setSmallEmptyText(R.string.empty_view_text_small_no_grades)
                    .setAdapter(adapter)
                    .setOnRefreshListener(::requestSyncWithRecyclerRefreshing)
                    .apply {
                        val layoutManager = LinearLayoutManager(context)
                        setLayoutManager(layoutManager)
                        recyclerView.addItemDecoration(
                                DividerItemDecoration(context, layoutManager.orientation)
                        )
                    }
        }

        fun unbindView() {
            recyclerManager = null
            adapter = null
            containerView = null
            clearFindViewByIdCache()
        }

        fun register(): Job {
            LocalBroadcast.registerReceiver(loginDataChangedReceiver,
                    intentFilter(GradesLoginData.getter))
            LocalBroadcast.registerReceiver(dataChangedReceiver,
                    intentFilter(GradesData.getter))

            return updateData()
        }

        fun unregister() {
            LocalBroadcast.unregisterReceiver(dataChangedReceiver)
            LocalBroadcast.unregisterReceiver(loginDataChangedReceiver)
        }

        private fun updateData(): Job = launch(UI) {
            userLoggedIn = accountHolder.accountId?.let {
                bg { GradesLoginData.loginData.isLoggedIn(it) }.await()
            } ?: false
            gradesMap = accountHolder.accountId?.let {
                bg { GradesData.instance.getGrades(it) }.await()
            }
            accountHolder.accountId?.let { accountId ->
                boxRecycler?.context?.let { context ->
                    // All grades changes will be displayed to user, so let's remove them all
                    gradesChangesMap.putAll(NotificationsManager
                            .cancelAll(context, GradesChangesNotificationGroup.ID,
                                    AccountNotificationChannel.idFor(accountId)))
                }
            }

            update()
        }

        fun update() {
            if (containerView == null) return

            boxRecycler.visibility = if (userLoggedIn) {
                adapter?.edit {
                    clear()
                    gradesMap?.let { it[semester.value] }?.let {
                        addAll(when (sort) {
                            GradesUiData.Sort.DATE -> it.map { GradeItem(it) }
                            GradesUiData.Sort.SUBJECTS -> it.toSubjects().map { SubjectItem(it) }
                        }) // TODO: highlight grades changes obtained from NotificationsManager
                    }
                }

                View.VISIBLE
            } else {
                adapter?.edit { clear() }

                View.GONE
            }
        }

        fun requestSyncWithLoading() {
            val account = accountHolder.account ?:
                    return longSnackbar(boxRecycler, R.string.snackbar_no_account_sync).show()
            val semester = semester
            val holder = holder

            launch(UI) {
                holder.showLoading()

                GradesSyncAdapter.requestSync(account, semester)

                // Sync sometimes reports, that is not started, at the beginning. So we must wait for sync start before we can wait for sync end.
                aWaitForSyncStart(account)
                aWaitForSyncEnd(account)

                //delay(500) // Wait few loops to make sure, that content was updated.
                holder.hideLoading()
            }
        }

        fun requestSyncWithRecyclerRefreshing() {
            val account = accountHolder.account ?:
                    return longSnackbar(boxRecycler, R.string.snackbar_no_account_sync).show()
            val semester = semester
            val recyclerManagerRef = recyclerManager?.asReference()

            launch(UI) {
                GradesSyncAdapter.requestSync(account, semester)

                // Sync sometimes reports, that is not started, at the beginning. So we must wait for sync start before we can wait for sync end.
                aWaitForSyncStart(account)
                aWaitForSyncEnd(account)

                recyclerManagerRef?.invoke()?.setRefreshing(false)
            }
        }
    }

    private class LayoutSyncStatus(private val accountHolder: ActiveAccountHolder,
                                   private val layoutLogin: LayoutLogin,
                                   private val layoutGrades: LayoutGrades) : LayoutContainer {

        companion object {
            private const val LOG_TAG = "${GradesFragment.LOG_TAG}\$LayoutSyncStatus"

        }

        override var containerView: View? = null

        private val loginDataChangedReceiver = broadcast { _, _ ->
            Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
            updateData()
        }
        private val dataChangedReceiver = broadcast { _, _ ->
            Log.d(LOG_TAG, "dataChangedReceiver.onReceive()")
            updateData()
        }

        private var syncLastResult: GradesData.SyncResult? = SUCCESS

        private var showingResult: GradesData.SyncResult? = SUCCESS
        private var statusSnackbar: Snackbar? = null

        init {
            accountHolder.addChangeListener { updateData() }
        }

        fun bindView(view: View) {
            containerView = view
        }

        fun unbindView() {
            containerView = null
            clearFindViewByIdCache()
        }

        fun register(): Job {
            LocalBroadcast.registerReceiver(loginDataChangedReceiver,
                    intentFilter(GradesLoginData.getter))
            LocalBroadcast.registerReceiver(dataChangedReceiver,
                    intentFilter(GradesData.getter))

            return updateData()
        }

        fun unregister() {
            LocalBroadcast.unregisterReceiver(dataChangedReceiver)
            LocalBroadcast.unregisterReceiver(loginDataChangedReceiver)
        }

        private fun updateData(): Job = launch(UI) {
            val userLoggedIn = accountHolder.accountId?.let {
                bg { GradesLoginData.loginData.isLoggedIn(it) }.await()
            } ?: false
            syncLastResult =
                    if (!userLoggedIn) SUCCESS // hide status info, if user is not logged in
                    else accountHolder.accountId?.let {
                        bg { GradesData.instance.getLastSyncResult(it) }.await()
                    }

            update()
        }

        fun update() {
            if (containerView == null || (showingResult == syncLastResult &&
                    statusSnackbar?.isShownOrQueued != false)) return

            when (syncLastResult) {
                FAIL_UNKNOWN -> statusSnackbar = indefiniteSnackbar(
                        baseView, R.string.snackbar_grades_refresh_fail_unknown
                ).apply {
                    setAction(R.string.snackbar_action_grades_retry) {
                        layoutGrades.requestSyncWithLoading()
                    }
                    show()
                }
                FAIL_CONNECT -> statusSnackbar = indefiniteSnackbar(
                        baseView, R.string.snackbar_grades_refresh_fail_connect
                ).apply {
                    setAction(R.string.snackbar_action_grades_retry) {
                        layoutGrades.requestSyncWithLoading()
                    }
                    show()
                }
                FAIL_LOGIN -> statusSnackbar = indefiniteSnackbar(
                        baseView, R.string.snackbar_grades_refresh_fail_login
                ).apply {
                    setAction(R.string.snackbar_action_grades_logout) { layoutLogin.logout() }
                    show()
                }
                null -> statusSnackbar = indefiniteSnackbar(
                        baseView, R.string.snackbar_grades_fail_no_account
                ).apply { show() }
                else -> statusSnackbar?.apply {
                    dismiss()
                    statusSnackbar = null
                }
            }

            showingResult = syncLastResult
        }
    }
}

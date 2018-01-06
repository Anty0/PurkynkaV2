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
import android.app.Activity
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
import cz.anty.purkynka.account.ActiveAccount
import cz.anty.purkynka.account.notify.AccountNotifyChannel
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.load.GradesParser.toSubjects
import cz.anty.purkynka.grades.notify.GradesChangesNotificationGroup
import cz.anty.purkynka.grades.notify.GradesChangesNotificationGroup.Companion.readDataChanges
import cz.anty.purkynka.grades.notify.GradesChangesNotificationGroup.Companion.readDataGrade
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesData.SyncResult.*
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesMap
import cz.anty.purkynka.grades.save.GradesUiData
import cz.anty.purkynka.grades.save.GradesUiData.Sort.*
import cz.anty.purkynka.grades.sync.GradesSyncAdapter
import cz.anty.purkynka.grades.ui.GradeItem
import cz.anty.purkynka.grades.ui.SubjectItem
import eu.codetopic.java.utils.JavaExtensions.runIfNull
import eu.codetopic.java.utils.JavaExtensions.kSerializer
import eu.codetopic.utils.AndroidExtensions.serialize
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.AndroidExtensions.deserializeBundle
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
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.PairSerializer
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import kotlinx.serialization.map
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

        private const val TIMEOUT_SYNC_ACTIVE = 10000L

        private const val SAVE_EXTRA_SORT = "$LOG_TAG.SORT"
        private const val SAVE_EXTRA_SEMESTER = "$LOG_TAG.SEMESTER"
        private const val SAVE_EXTRA_CHANGES_MAP = "$LOG_TAG.CHANGES_MAP"

        suspend fun aWaitForSyncStart(account: Account) = aWaitForSyncState {
            (ContentResolver.isSyncActive(account, GradesSyncAdapter.CONTENT_AUTHORITY) ||
                    ContentResolver.isSyncPending(account, GradesSyncAdapter.CONTENT_AUTHORITY))
                    .also { Log.d(LOG_TAG, "aWaitForSyncStart(account=$account) -> $it") }
        }

        suspend fun aWaitForSyncActiveOrEnd(account: Account) = withTimeoutOrNull(TIMEOUT_SYNC_ACTIVE) {
            aWaitForSyncState {
                (ContentResolver.isSyncActive(account, GradesSyncAdapter.CONTENT_AUTHORITY) ||
                        !ContentResolver.isSyncPending(account, GradesSyncAdapter.CONTENT_AUTHORITY))
                        .also {
                            Log.d(LOG_TAG, "aWaitForSyncActiveOrEnd(account=$account)" +
                                    " -> Condition result: $it")
                        }
            }
        }.runIfNull {
            Log.d(LOG_TAG, "aWaitForSyncActiveOrEnd(account=$account) -> Timeout reached")
        } != null

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
    }

    override val title: CharSequence
        get() = getText(R.string.action_show_grades)
    override val themeId: Int
        get() = R.style.AppTheme_Grades

    private val accountHolder = ActiveAccountHolder()
    private val layLogin = LayoutLogin(accountHolder, holder)
    private val layGrades = LayoutGrades(accountHolder, layLogin, holder)
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

        layGrades.bindView(activity, themedContext, themedInflater, view)
        layLogin.bindView(view)
        laySyncStatus.bindView(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launch(UI) {
            holder.showLoading()

            // TODO: Create superclass for these layouts and call onEach register
            arrayOf(
                    layLogin.updateData(),
                    layGrades.updateData(),
                    laySyncStatus.updateData(),
                    accountHolder.updateData()
            ).forEach { it.join() }

            holder.hideLoading()
        }
    }

    override fun onStart() {
        super.onStart()

        layLogin.register()
        layGrades.register()
        laySyncStatus.register()
        accountHolder.register()
    }

    override fun onStop() {
        accountHolder.unregister()
        laySyncStatus.unregister()
        layGrades.unregister()
        layLogin.unregister()

        super.onStop()
    }

    override fun onDestroyView() {
        laySyncStatus.unbindView()
        layLogin.unbindView()
        layGrades.unbindView()

        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        layGrades.createOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item)) return true

        return layGrades.optionsItemSelected(item)
    }

    private class ActiveAccountHolder {

        companion object {
            private const val LOG_TAG = "${GradesFragment.LOG_TAG}\$ActiveAccountHolder"

        }

        private val accountChangedReceiver = broadcast { _, _ ->
            Log.d(LOG_TAG, "accountChangedReceiver.onReceive()")
            updateData()
        }

        private val listeners = mutableListOf<suspend () -> Unit>()

        var account: Account? = null
            private set
        var accountId: String? = null
            private set

        fun updateData(): Job {
            val self = this.asReference()

            return launch(UI) {
                val accountWithId = bg { ActiveAccount.getWithId() }.await()
                val nAccount = accountWithId.first
                val nAccountId = accountWithId.second

                self().apply {
                    account = nAccount
                    accountId = nAccountId

                    listeners.forEach { it() }
                }
            }
        }

        fun addChangeListener(listener: suspend () -> Unit) {
            listeners.add(listener)
        }

        fun register(): Job {
            LocalBroadcast.registerReceiver(accountChangedReceiver,
                    intentFilter(ActiveAccount.getter))

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
            accountHolder.addChangeListener { updateData().join() }
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

        fun updateData(): Job = launch(UI) {
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
                if (aWaitForSyncActiveOrEnd(account)) {
                    aWaitForSyncEnd(account)
                } else {
                    longSnackbar(baseView, R.string.snackbar_sync_start_fail).show()
                    bg { GradesLoginData.loginData.logout(accountId) }.await()
                }

                delay(500) // Wait few loops to make sure, that content was updated.
                holder.hideLoading() // FIXME: why is loading being hidden hidden before grades are loaded???
            }
        }

        fun logout() {
            //val account = accountHolder.account ?:
            //        return longSnackbar(boxLogin, R.string.snackbar_no_account_logout).show()
            val accountId = accountHolder.accountId ?:
                    return longSnackbar(boxLogin, R.string.snackbar_no_account_logout).show()

            launch(UI) {
                holder.showLoading()

                bg { GradesLoginData.loginData.logout(accountId) }.await()

                delay(500) // Wait few loops to make sure, that content was updated.
                holder.hideLoading()
            }
        }
    }

    private class LayoutGrades(private val accountHolder: ActiveAccountHolder,
                               private val layoutLogin: LayoutLogin,
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
        private var gradesChangesMap: MutableMap<Int, List<String>> = mutableMapOf()

        private var activity: Activity? = null
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
            accountHolder.addChangeListener { updateData().join() }
        }

        fun restore(savedInstanceState: Bundle?) {
            savedInstanceState?.takeIf { it.containsKey(SAVE_EXTRA_SORT) }?.let {
                sort = try {
                    savedInstanceState.getSerializable(SAVE_EXTRA_SORT) as GradesUiData.Sort
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "onCreate() -> restoreSortState()", e)
                    GradesUiData.Sort.GRADES_DATE
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
            savedInstanceState?.takeIf { it.containsKey(SAVE_EXTRA_CHANGES_MAP) }?.let {
                gradesChangesMap = try {
                    savedInstanceState.getString(SAVE_EXTRA_CHANGES_MAP)?.let {
                        JSON.parse((IntSerializer to StringSerializer.list).map, it)
                    }?.toMutableMap() ?: throw RuntimeException()
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "onCreate() -> restoreChangesMapState()", e)
                    mutableMapOf()
                }
            }
        }

        fun save(outState: Bundle) {
            outState.putSerializable(SAVE_EXTRA_SORT, sort)
            outState.putSerializable(SAVE_EXTRA_SEMESTER, semester)
            outState.putString(SAVE_EXTRA_CHANGES_MAP,
                    JSON.stringify(
                            (IntSerializer to StringSerializer.list).map,
                            gradesChangesMap
                    )
            )
        }

        fun bindView(activity: Activity?, context: Context, inflater: LayoutInflater, view: View) {
            containerView = view.boxRecycler

            this.activity = activity

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
            activity = null
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

        fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
            if (!userLoggedIn) return

            inflater.inflate(R.menu.fragment_grades_menu, menu)

            menu.findItem(
                    when(sort) {
                        GRADES_DATE -> R.id.action_show_grades_date
                        GRADES_VALUE -> R.id.action_show_grades_value
                        GRADES_SUBJECT -> R.id.action_show_grades_subject
                        SUBJECTS_NAME -> R.id.action_show_subjects_name
                        SUBJECTS_AVERAGE_BEST -> R.id.action_show_subjects_average_best
                        SUBJECTS_AVERAGE_WORSE -> R.id.action_show_subjects_average_worse
                    }
            ).isChecked = true

            menu.findItem(
                    if (semester == Semester.FIRST) R.id.action_semester_first
                    else R.id.action_semester_second
            ).isChecked = true
        }

        fun optionsItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_show_grades_date -> {
                    item.isChecked = true
                    sort = GRADES_DATE
                }
                R.id.action_show_grades_value -> {
                    item.isChecked = true
                    sort = GRADES_VALUE
                }
                R.id.action_show_grades_subject -> {
                    item.isChecked = true
                    sort = GRADES_SUBJECT
                }
                R.id.action_show_subjects_name -> {
                    item.isChecked = true
                    sort = SUBJECTS_NAME
                }
                R.id.action_show_subjects_average_best -> {
                    item.isChecked = true
                    sort = SUBJECTS_AVERAGE_BEST
                }
                R.id.action_show_subjects_average_worse -> {
                    item.isChecked = true
                    sort = SUBJECTS_AVERAGE_WORSE
                }
                R.id.action_semester_first -> {
                    item.isChecked = true
                    semester = Semester.FIRST
                }
                R.id.action_semester_second -> {
                    item.isChecked = true
                    semester = Semester.SECOND
                }
                R.id.action_refresh -> requestSyncWithLoading()
                R.id.action_log_out -> layoutLogin.logout()
                else -> return false
            }
            return true
        }

        fun updateData(): Job = launch(UI) {
            userLoggedIn = accountHolder.accountId?.let {
                bg { GradesLoginData.loginData.isLoggedIn(it) }.await()
            } ?: false
            gradesMap = accountHolder.accountId?.let {
                bg { GradesData.instance.getGrades(it) }.await()
            }
            accountHolder.accountId?.let { accountId ->
                boxRecycler?.context?.let { context ->
                    // All grades changes will be displayed to user, so let's remove them all
                    gradesChangesMap.putAll(
                            NotificationsManager.cancelAll(
                                    context,
                                    GradesChangesNotificationGroup.ID,
                                    AccountNotifyChannel.idFor(accountId)
                            ).mapNotNull {
                                (readDataGrade(it.value)?.id ?: return@mapNotNull null) to
                                        (readDataChanges(it.value) ?: return@mapNotNull null)
                            }
                    )
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
                        val grades = {
                            it.map {
                                GradeItem(it, changes = gradesChangesMap[it.id])
                            }
                        }
                        val subjects = {
                            it.toSubjects().map {
                                SubjectItem(it, it.grades.mapNotNull { grade ->
                                    gradesChangesMap[grade.id]?.let { grade.id to it }
                                }.toMap())
                            }
                        }
                        addAll(when (sort) {
                            GRADES_DATE -> run(grades)
                            GRADES_VALUE -> run(grades).sortedBy { it.base.value }
                            GRADES_SUBJECT -> run(grades).sortedBy { it.base.subjectShort }
                            SUBJECTS_NAME -> run(subjects)
                            SUBJECTS_AVERAGE_BEST -> run(subjects).sortedBy { it.base.average }
                            SUBJECTS_AVERAGE_WORSE ->
                                run(subjects).sortedByDescending { it.base.average }
                        })
                    }
                }

                View.VISIBLE
            } else {
                adapter?.edit { clear() }

                View.GONE
            }

            activity?.invalidateOptionsMenu()
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
                if (aWaitForSyncActiveOrEnd(account)) {
                    aWaitForSyncEnd(account)
                } else {
                    longSnackbar(baseView, R.string.snackbar_sync_start_fail).show()
                }

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
                if (aWaitForSyncActiveOrEnd(account)) {
                    aWaitForSyncEnd(account)
                } else {
                    longSnackbar(baseView, R.string.snackbar_sync_start_fail).show()
                }

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
            accountHolder.addChangeListener { updateData().join() }
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

        fun updateData(): Job = launch(UI) {
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

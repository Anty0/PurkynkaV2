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

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import cz.anty.purkynka.utils.ICON_GRADES

import cz.anty.purkynka.R
import cz.anty.purkynka.utils.*
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.load.GradesParser.toSubjects
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel.Companion.readDataChanges
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel.Companion.readDataGrade
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesData.SyncResult.*
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesMap
import cz.anty.purkynka.grades.save.GradesUiData
import cz.anty.purkynka.grades.save.GradesUiData.Sort.*
import cz.anty.purkynka.grades.sync.GradesSyncAdapter
import cz.anty.purkynka.grades.data.Subject.Companion.average
import cz.anty.purkynka.grades.ui.GradeItem
import cz.anty.purkynka.grades.ui.SubjectItem
import eu.codetopic.java.utils.ifFalse
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.receiver
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.edit
import eu.codetopic.utils.getIconics
import eu.codetopic.utils.getKSerializableExtra
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.bundle.BundleSerializer
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.ui.activity.fragment.IconProvider
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
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import kotlinx.serialization.map
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.ctx
import proguard.annotation.KeepName

/**
 * @author anty
 */
@KeepName
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class GradesFragment : NavigationFragment(), TitleProvider, ThemeProvider, IconProvider {

    companion object {

        private const val LOG_TAG = "GradesFragment"
    }

    override val title: CharSequence
        get() = getText(R.string.title_fragment_grades)
    override val themeId: Int
        get() = R.style.AppTheme_Grades
    override val icon: Bitmap
        get() = ctx.getIconics(ICON_GRADES).sizeDp(48).toBitmap()

    private val accountHolder = ActiveAccountHolder(holder)
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
        val themedContext = ContextThemeWrapper(inflater.context, themeId)
        val themedInflater = inflater.cloneInContext(themedContext)
        val view = themedInflater.inflate(R.layout.fragment_grades, container, false)

        layGrades.bindView(activity, themedContext, themedInflater, view)
        layLogin.bindView(view)
        laySyncStatus.bindView(view)

        return view
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        launch(UI) {
            holder.showLoading()

            // TODO: Create superclass for these layouts and call onEach update
            arrayOf(
                    layLogin.update(),
                    layGrades.update(),
                    laySyncStatus.update(),
                    accountHolder.update()
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
        return when {
            layGrades.optionsItemSelected(item) -> true
            super.onOptionsItemSelected(item) -> true
            else -> false
        }
    }

    @ContainerOptions(CacheImplementation.SPARSE_ARRAY)
    private class LayoutLogin(private val accountHolder: ActiveAccountHolder,
                              private val holder: LoadingVH) : LayoutContainer {

        companion object {
            private const val LOG_TAG = "${GradesFragment.LOG_TAG}\$LayoutLogin"

        }

        override var containerView: View? = null

        private val loginDataChangedReceiver = receiver { _, _ ->
            Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
            updateWithLoading()
        }

        private var userLoggedIn = false
        private var username = ""

        init {
            accountHolder.addChangeListener { update().join() }
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

            return update()
        }

        fun unregister() {
            LocalBroadcast.unregisterReceiver(loginDataChangedReceiver)
        }

        private fun updateWithLoading(): Job {
            val holderRef = holder.asReference()
            return launch(UI) {
                holderRef().showLoading()

                update().join()

                holderRef().hideLoading()
            }
        }

        fun update(): Job = launch(UI) {
            userLoggedIn = accountHolder.accountId?.let {
                bg { GradesLoginData.loginData.isLoggedIn(it) }.await()
            } ?: false
            username = accountHolder.accountId?.let {
                bg { GradesLoginData.loginData.getUsername(it) }.await()
            } ?: ""

            updateUi()
        }

        fun updateUi() {
            containerView ?: return

            boxLogin.visibility = if (!userLoggedIn) {
                inUsername.takeIf { it.text.isEmpty() }?.setText(username)

                View.VISIBLE
            } else View.GONE
        }

        fun login() {
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

            val boxLoginRef = boxLogin.asReference()
            launch(UI) {
                holder.showLoading()

                bg {
                    GradesData.instance.resetFirstSyncState(accountId)
                    GradesLoginData.loginData.login(accountId, username, password)
                }.await()

                // Sync will be triggered later by login change broadcast
                if (awaitForSyncCompleted(account, GradesSyncAdapter.CONTENT_AUTHORITY)) {
                    val syncResult = bg { GradesData.instance.getLastSyncResult(accountId) }.await()
                    if (syncResult == FAIL_LOGIN) {
                        longSnackbar(boxLoginRef(), R.string.snackbar_grades_login_fail)
                        bg { GradesLoginData.loginData.logout(accountId) }.await()
                    }
                } else {
                    longSnackbar(boxLoginRef(), R.string.snackbar_sync_start_fail)
                    bg { GradesLoginData.loginData.logout(accountId) }.await()
                }

                delay(500) // Wait few loops to make sure, that content was updated.
                holder.hideLoading()
            }
        }

        fun logout() {
            val accountId = accountHolder.accountId ?: run {
                longSnackbar(boxLogin, R.string.snackbar_no_account_logout)
                return
            }

            val appContext = boxLogin.context.applicationContext
            launch(UI) {
                holder.showLoading()

                bg {
                    GradesLoginData.loginData.logout(accountId)
                    GradesData.instance.resetFirstSyncState(accountId)
                }.await()

                NotifyManager.requestCancelAll(
                        context = appContext,
                        groupId = AccountNotifyGroup.idFor(accountId),
                        channelId = GradesChangesNotifyChannel.ID
                )

                delay(500) // Wait few loops to make sure, that content was updated.
                holder.hideLoading()
            }
        }
    }

    @ContainerOptions(CacheImplementation.SPARSE_ARRAY)
    private class LayoutGrades(private val accountHolder: ActiveAccountHolder,
                               private val layoutLogin: LayoutLogin,
                               private val holder: LoadingVH) : LayoutContainer {

        companion object {

            private const val LOG_TAG = "${GradesFragment.LOG_TAG}\$LayoutGrades"

            private const val SAVE_EXTRA_SEMESTER = "$LOG_TAG.SEMESTER"
            private const val SAVE_EXTRA_CHANGES_MAP = "$LOG_TAG.CHANGES_MAP"

            private val gradesChangesMapSerializer =
                    (StringSerializer to (IntSerializer to StringSerializer.list).map).map
        }

        override var containerView: View? = null

        private val loginDataChangedReceiver = receiver { _, _ ->
            Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
            updateWithLoading()
        }
        private val dataChangedReceiver = receiver { _, _ ->
            Log.d(LOG_TAG, "dataChangedReceiver.onReceive()")
            update()
        }
        private val uiDataChangedReceiver = receiver { _, _ ->
            Log.d(LOG_TAG, "uiDataChangedReceiver.onReceive()")
            update()
        }
        private val newGradesChangesReceiver = receiver { _, intent ->
            Log.d(LOG_TAG, "newGradesChangesReceiver.onReceive()")

            val accountId = intent
                    ?.getStringExtra(GradesSyncAdapter.EXTRA_ACCOUNT_ID)
                    ?: return@receiver
            val gradesChanges = intent
                    .getKSerializableExtra(
                            GradesSyncAdapter.EXTRA_GRADES_CHANGES,
                            BundleSerializer.list
                    ) ?: return@receiver

            addGradesChanges(accountId, gradesChanges)

            updateUi()

            abortBroadcast()
        }

        private var userLoggedIn = false
        private var gradesMap: GradesMap? = null
        private var gradesChangesMap: MutableMap<String, MutableMap<Int, List<String>>> = mutableMapOf()

        private var activity: Activity? = null
        private var recyclerManager: Recycler.RecyclerManagerImpl? = null
        private var adapter: CustomItemAdapter<CustomItem>? = null

        var sort: GradesUiData.Sort? = null
        var semester: Semester = Semester.AUTO.stableSemester
            set(value) {
                field = value
                updateUi()
            }

        init {
            accountHolder.addChangeListener { update().join() }
        }

        fun restore(savedInstanceState: Bundle?) {
            savedInstanceState?.takeIf { it.containsKey(SAVE_EXTRA_SEMESTER) }?.let {
                semester = try {
                    savedInstanceState.getSerializable(SAVE_EXTRA_SEMESTER) as Semester
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "onCreate() -> restoreSemesterState()", e)
                    Semester.AUTO.stableSemester
                }
            }
            savedInstanceState?.takeIf { it.containsKey(SAVE_EXTRA_CHANGES_MAP) }?.let {
                gradesChangesMap = try {
                    savedInstanceState.getString(SAVE_EXTRA_CHANGES_MAP)
                            ?.let { JSON.parse(gradesChangesMapSerializer, it) }
                            ?.map { it.key to it.value.toMutableMap() }
                            ?.toMap()?.toMutableMap()
                            ?: throw RuntimeException()
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "onCreate() -> restoreChangesMapState()", e)
                    mutableMapOf()
                }
            }
        }

        fun save(outState: Bundle) {
            outState.putSerializable(SAVE_EXTRA_SEMESTER, semester)
            outState.putString(SAVE_EXTRA_CHANGES_MAP,
                    JSON.stringify(
                            gradesChangesMapSerializer,
                            gradesChangesMap
                    )
            )
        }

        fun bindView(activity: Activity?, context: Context, inflater: LayoutInflater, view: View) {
            containerView = view.boxGrades

            this.activity = activity

            adapter = CustomItemAdapter(context)

            recyclerManager = Recycler.inflate().withSwipeToRefresh().withItemDivider()
                    .on(inflater, boxRecycler, true)
                    .setEmptyImage(context.getIconics(ICON_GRADES).sizeDp(72))
                    .setEmptyText(R.string.empty_view_text_no_grades)
                    .setSmallEmptyText(R.string.empty_view_text_small_no_grades)
                    .setAdapter(adapter)
                    .setOnRefreshListener(::requestSyncWithRecyclerRefreshing)
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
            LocalBroadcast.registerReceiver(uiDataChangedReceiver,
                    intentFilter(GradesUiData.getter))
            boxRecycler.context.registerReceiver(newGradesChangesReceiver,
                    intentFilter(GradesSyncAdapter.ACTION_NEW_GRADES_CHANGES))

            return update()
        }

        fun unregister() {
            boxRecycler.context.unregisterReceiver(newGradesChangesReceiver)
            LocalBroadcast.unregisterReceiver(uiDataChangedReceiver)
            LocalBroadcast.unregisterReceiver(dataChangedReceiver)
            LocalBroadcast.unregisterReceiver(loginDataChangedReceiver)
        }

        fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
            if (!userLoggedIn) return

            inflater.inflate(R.menu.fragment_grades, menu)

            menu.findItem(R.id.menu_group_filter_grades).icon = activity
                    ?.getIconics(GoogleMaterial.Icon.gmd_filter_list)
                    ?.actionBar()

            menu.findItem(R.id.action_refresh).icon = activity
                    ?.getIconics(GoogleMaterial.Icon.gmd_refresh)
                    ?.actionBar()

            menu.findItem(R.id.action_log_out).icon = activity
                    ?.getIconics(CommunityMaterial.Icon.cmd_logout)
                    ?.actionBar()

            menu.findItem(
                    when(sort) {
                        GRADES_DATE -> R.id.action_show_grades_date
                        GRADES_VALUE -> R.id.action_show_grades_value
                        GRADES_SUBJECT -> R.id.action_show_grades_subject
                        SUBJECTS_NAME -> R.id.action_show_subjects_name
                        SUBJECTS_AVERAGE_BEST -> R.id.action_show_subjects_average_best
                        SUBJECTS_AVERAGE_WORSE -> R.id.action_show_subjects_average_worse
                        else -> R.id.action_show_grades_date
                    }
            ).isChecked = true

            menu.findItem(
                    if (semester == Semester.FIRST) R.id.action_semester_first
                    else R.id.action_semester_second
            ).isChecked = true
        }

        fun optionsItemSelected(item: MenuItem): Boolean {
            val accountId = accountHolder.accountId ?: return false
            when (item.itemId) {
                R.id.action_show_grades_date -> {
                    item.isChecked = true
                    GradesUiData.instance.setLastSort(accountId, GRADES_DATE)
                }
                R.id.action_show_grades_value -> {
                    item.isChecked = true
                    GradesUiData.instance.setLastSort(accountId, GRADES_VALUE)
                }
                R.id.action_show_grades_subject -> {
                    item.isChecked = true
                    GradesUiData.instance.setLastSort(accountId, GRADES_SUBJECT)
                }
                R.id.action_show_subjects_name -> {
                    item.isChecked = true
                    GradesUiData.instance.setLastSort(accountId, SUBJECTS_NAME)
                }
                R.id.action_show_subjects_average_best -> {
                    item.isChecked = true
                    GradesUiData.instance.setLastSort(accountId, SUBJECTS_AVERAGE_BEST)
                }
                R.id.action_show_subjects_average_worse -> {
                    item.isChecked = true
                    GradesUiData.instance.setLastSort(accountId, SUBJECTS_AVERAGE_WORSE)
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

        private fun updateWithLoading(): Job {
            val holderRef = holder.asReference()
            return launch(UI) {
                holderRef().showLoading()

                update().join()

                holderRef().hideLoading()
            }
        }

        fun addGradesChanges(accountId: String, changes: Iterable<Bundle>) = gradesChangesMap
                .getOrPut(accountId, ::mutableMapOf)
                .putAll(
                        changes.mapNotNull {
                            readDataGrade(it)?.id?.let { id ->
                                readDataChanges(it)?.let { changes ->
                                    id to changes
                                }
                            }
                        }
                )

        fun update(): Job = launch(UI) {
            sort = accountHolder.accountId?.let {
                bg { GradesUiData.instance.getLastSort(it) }.await()
            }
            userLoggedIn = accountHolder.accountId?.let {
                bg { GradesLoginData.loginData.isLoggedIn(it) }.await()
            } ?: false
            gradesMap = accountHolder.accountId?.let {
                bg { GradesData.instance.getGrades(it) }.await()
            }
            accountHolder.accountId?.let { accountId ->
                // All grades changes will be displayed to user, so let's remove them all
                NotifyManager
                        .requestSuspendCancelAll(
                                context = boxRecycler?.context ?: return@let,
                                groupId = AccountNotifyGroup.idFor(accountId),
                                channelId = GradesChangesNotifyChannel.ID
                        )
                        .takeIf { it.isNotEmpty() }
                        ?.also { addGradesChanges(accountId, it.values) }
            }

            updateUi()
        }

        fun updateUi() {
            containerView ?: return

            boxGrades.visibility = if (userLoggedIn) {
                adapter?.edit {
                    clear()
                    accountHolder.accountId?.let { accountId ->
                        gradesMap?.let { it[semester.value] }?.let {
                            val grades = {
                                it.map {
                                    GradeItem(it, changes = gradesChangesMap[accountId]?.get(it.id))
                                }
                            }
                            val subjects = {
                                it.toSubjects().map {
                                    SubjectItem(it, it.grades.mapNotNull { grade ->
                                        gradesChangesMap[accountId]?.get(grade.id)
                                                ?.let { grade.id to it }
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
                                else -> run(grades)
                            })
                        }
                    }
                    notifyAllItemsChanged()
                }

                View.VISIBLE
            } else {
                adapter?.edit { clear() }

                View.GONE
            }

            // Allow menu visibility changes based on userLoggedIn state
            activity?.invalidateOptionsMenu()
        }

        fun requestSyncWithLoading() {
            val account = accountHolder.account ?: run {
                longSnackbar(boxGrades, R.string.snackbar_no_account_sync)
                return
            }
            val semester = semester
            val boxGradesRef = boxGrades.asReference()
            val holder = holder

            launch(UI) {
                holder.showLoading()

                GradesSyncAdapter.requestSync(account, semester)

                awaitForSyncCompleted(account, GradesSyncAdapter.CONTENT_AUTHORITY) ifFalse {
                    longSnackbar(boxGradesRef(), R.string.snackbar_sync_start_fail)
                }

                //delay(500) // Wait few loops to make sure, that content was updated.
                holder.hideLoading()
            }
        }

        fun requestSyncWithRecyclerRefreshing() {
            val account = accountHolder.account ?: run {
                longSnackbar(boxGrades, R.string.snackbar_no_account_sync)
                return
            }
            val semester = semester
            val boxGradesRef = boxGrades.asReference()
            val recyclerManagerRef = recyclerManager?.asReference() ?: return

            launch(UI) {
                GradesSyncAdapter.requestSync(account, semester)

                awaitForSyncCompleted(account, GradesSyncAdapter.CONTENT_AUTHORITY) ifFalse {
                    longSnackbar(boxGradesRef(), R.string.snackbar_sync_start_fail)
                }

                recyclerManagerRef().setRefreshing(false)
            }
        }
    }

    @ContainerOptions(CacheImplementation.SPARSE_ARRAY)
    private class LayoutSyncStatus(private val accountHolder: ActiveAccountHolder,
                                   private val layoutLogin: LayoutLogin,
                                   private val layoutGrades: LayoutGrades) : LayoutContainer {

        companion object {

            private const val LOG_TAG = "${GradesFragment.LOG_TAG}\$LayoutSyncStatus"
        }

        override var containerView: View? = null

        private val loginDataChangedReceiver = receiver { _, _ ->
            Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
            update()
        }
        private val dataChangedReceiver = receiver { _, _ ->
            Log.d(LOG_TAG, "dataChangedReceiver.onReceive()")
            update()
        }

        private var syncLastResult: GradesData.SyncResult? = SUCCESS

        private var showingResult: GradesData.SyncResult? = SUCCESS
        private var statusSnackbar: Snackbar? = null

        init {
            accountHolder.addChangeListener { update().join() }
        }

        fun bindView(view: View) {
            containerView = view.boxGrades
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

            return update()
        }

        fun unregister() {
            LocalBroadcast.unregisterReceiver(dataChangedReceiver)
            LocalBroadcast.unregisterReceiver(loginDataChangedReceiver)
        }

        fun update(): Job = launch(UI) {
            val userLoggedIn = accountHolder.accountId?.let {
                bg { GradesLoginData.loginData.isLoggedIn(it) }.await()
            } ?: false
            syncLastResult =
                    if (!userLoggedIn) SUCCESS // hide status info, if user is not logged in
                    else accountHolder.accountId?.let {
                        bg { GradesData.instance.getLastSyncResult(it) }.await()
                    }

            updateUi()
        }

        fun updateUi(force: Boolean = false) {
            containerView ?: return

            if (!force && showingResult == syncLastResult &&
                    statusSnackbar?.isShownOrQueued != false) return

            when (syncLastResult) {
                FAIL_UNKNOWN -> statusSnackbar = indefiniteSnackbar(
                        boxGrades,
                        R.string.snackbar_grades_refresh_fail_unknown,
                        R.string.snackbar_action_grades_retry,
                        {
                            layoutGrades.requestSyncWithLoading()
                            updateUi(true)
                        }
                )
                FAIL_CONNECT -> statusSnackbar = indefiniteSnackbar(
                        boxGrades,
                        R.string.snackbar_grades_refresh_fail_connect,
                        R.string.snackbar_action_grades_retry,
                        {
                            layoutGrades.requestSyncWithLoading()
                            updateUi(true)
                        }
                )
                FAIL_LOGIN -> statusSnackbar = indefiniteSnackbar(
                        boxGrades,
                        R.string.snackbar_grades_refresh_fail_login,
                        R.string.snackbar_action_grades_logout,
                        {
                            layoutLogin.logout()
                            updateUi(true)
                        }
                )
                null -> statusSnackbar = indefiniteSnackbar(
                        boxGrades,
                        R.string.snackbar_grades_fail_no_account
                )
                else -> {
                    statusSnackbar?.apply {
                        dismiss()
                        statusSnackbar = null
                    }
                }
            }

            showingResult = syncLastResult
        }
    }
}

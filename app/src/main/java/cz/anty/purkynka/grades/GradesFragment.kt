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
import android.content.SyncStatusObserver
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.Toast

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import cz.anty.purkynka.R
import cz.anty.purkynka.accounts.AccountsHelper
import cz.anty.purkynka.accounts.ActiveAccountManager
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesData.SyncResult.*
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesSyncAdapter
import cz.anty.purkynka.grades.ui.StandaloneGradeItem
import cz.anty.purkynka.grades.ui.StandaloneSubjectItem
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.LocalBroadcast
import eu.codetopic.utils.AndroidExtensions.edit
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.thread.JobUtils
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import eu.codetopic.utils.ui.container.adapter.CustomItemAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.recycler.Recycler
import eu.codetopic.utils.ui.container.recycler.utils.RecyclerItemClickListener.SimpleClickListener

/**
 * @author anty
 */
class GradesFragment : NavigationFragment(), TitleProvider, ThemeProvider {

    companion object {

        private const val LOG_TAG = "GradesFragment"

        private const val SAVE_EXTRA_SORT = "$LOG_TAG.SORT"
        private const val SAVE_EXTRA_SEMESTER = "$LOG_TAG.SEMESTER"
    }

    @BindView(R.id.base_view)
    lateinit var baseView: FrameLayout

    @BindView(R.id.container_recycler)
    lateinit var recyclerContainer: FrameLayout
    @BindView(R.id.container_login)
    lateinit var loginContainer: ScrollView

    @BindView(R.id.edit_username)
    lateinit var inputUsername: EditText
    @BindView(R.id.edit_password)
    lateinit var inputPassword: EditText

    private var unbinder: Unbinder? = null

    private var recyclerManager: Recycler.RecyclerManagerImpl? = null
    private var adapter: CustomItemAdapter<CustomItem>? = null

    private val gradesData = GradesData.instance
    private val loginData = GradesLoginData.loginData
    private val activeAccountManager = ActiveAccountManager.instance

    private var activeAccount: Account? = null
    private var activeAccountId: String? = null

    private var statusSnackbar: Snackbar? = null
    private var sort: Sort = Sort.DATE
        set(value) {
            field = value
            if (unbinder != null) updateRecycler()
        }
    private var semester: Semester = Semester.AUTO.stableSemester
        set(value) {
            field = value
            if (unbinder != null) updateRecycler()
        }

    private var syncObserverHandle: Any? = null

    private val accountChangedReceiver = broadcast { _, _ -> updateActiveAccount() }
    private val loginDataChangedReceiver = broadcast { _, _ -> if (unbinder != null) updateVisibility() }
    private val dataChangedReceiver = broadcast { _, _ ->
        if (unbinder == null) return@broadcast
        updateRecycler()
        updateLoading()
    }
    private val syncObserver = SyncStatusObserver {
        JobUtils.runOnMainThread {
            if (unbinder == null) return@runOnMainThread
            updateRecycler()
            updateLoading()
        }
    }

    init {
        setHasOptionsMenu(true)
    }

    private fun showNoAccountSnackbar(@StringRes messageId: Int) =
            showNoAccountSnackbar(getText(messageId))

    private fun showNoAccountSnackbar(message: CharSequence) {
        if (unbinder == null) return Log.w(LOG_TAG,
                "showNoAccountSnackbar($message): Can't show snackbar, no view available.")
        Snackbar.make(baseView, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.takeIf { it.containsKey(SAVE_EXTRA_SORT) }?.let {
            sort = try {
                savedInstanceState.getSerializable(SAVE_EXTRA_SORT) as Sort
            } catch (e: Exception) {
                Log.w(LOG_TAG, "onCreate() -> restoreSortState()", e)
                Sort.DATE
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

        adapter = CustomItemAdapter(context)

        LocalBroadcast.registerReceiver(loginDataChangedReceiver, intentFilter(GradesLoginData.getter))
        LocalBroadcast.registerReceiver(dataChangedReceiver, intentFilter(GradesData.getter))
        LocalBroadcast.registerReceiver(accountChangedReceiver, intentFilter(ActiveAccountManager.getter))


        val mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING or ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
        syncObserverHandle = ContentResolver.addStatusChangeListener(mask, syncObserver)

        updateActiveAccount()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(SAVE_EXTRA_SORT, sort)
        outState.putSerializable(SAVE_EXTRA_SEMESTER, semester)
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val themedInflater = inflater.cloneInContext(
                ContextThemeWrapper(inflater.context, R.style.AppTheme_Grades))
        val baseView = themedInflater.inflate(R.layout.fragment_grades, container, false)
        unbinder = ButterKnife.bind(this, baseView)

        recyclerManager = Recycler.inflate().withSwipeToRefresh()
                .on(themedInflater, recyclerContainer, true)
                .setEmptyImage(R.mipmap.ic_launcher_grades) // TODO: better image
                .setEmptyText(R.string.empty_view_text_no_grades)
                .setAdapter(adapter)
                .setOnRefreshListener(::requestSync)

        updateVisibility()
        return baseView
    }

    @OnClick(R.id.but_login)
    fun onLoginClick() = login()

    private fun requestSyncWithLoading() {
        holder.takeIf { !it.isLoadingShowed }?.showLoading()
        requestSync()
    }

    private fun requestSync() {
        GradesSyncAdapter.requestSync(
                activeAccount ?: return showNoAccountSnackbar(
                        R.string.snackbar_no_account_sync),
                semester
        )
    }

    private fun login() {
        holder.takeIf { !it.isLoadingShowed }?.showLoading()
        loginData.login(
                activeAccountId ?: return showNoAccountSnackbar(
                        R.string.snackbar_no_account_login),
                inputUsername.text.toString(),
                inputPassword.text.toString()
        )
    }

    private fun logout() {
        holder.takeIf { !it.isLoadingShowed }?.showLoading()
        loginData.logout(
                activeAccountId ?: return showNoAccountSnackbar(
                        R.string.snackbar_no_account_login)
        )
    }

    private fun updateActiveAccount() {
        val account = activeAccountManager.activeAccount
        val accountId = if (account == null) null else
            AccountsHelper.getAccountId(context, account)

        activeAccount = account
        activeAccountId = accountId

        // Check if views are initialized, fixes possible problems
        // if data change occur after onCreate(), but before onCreateContentView()
        if (unbinder == null) return

        updateVisibility()
    }

    private fun updateVisibility() {
        val accountId = activeAccountId
        if (accountId != null && loginData.isLoggedIn(accountId)) {
            recyclerContainer.visibility = View.VISIBLE
            loginContainer.visibility = View.GONE

            updateRecycler()
        } else {
            recyclerContainer.visibility = View.GONE
            loginContainer.visibility = View.VISIBLE

            updateLoginScreen()
        }
        updateLoading()
    }

    private fun updateLoading() {
        val account = activeAccount
        if (account == null
                || (!ContentResolver.isSyncActive(account, GradesSyncAdapter.CONTENT_AUTHORITY)
                && !ContentResolver.isSyncPending(account, GradesSyncAdapter.CONTENT_AUTHORITY))) {
            holder.takeIf { it.isLoadingShowed }?.hideLoading()
            recyclerManager?.setRefreshing(false)
        }
    }

    private fun updateLoginScreen() {
        val accountId = activeAccountId
        if (accountId != null && inputUsername.text.isEmpty())
            loginData.getUsername(accountId)?.let { inputUsername.setText(it) }
    }

    private fun updateRecycler() {
        adapter?.edit {
            clear()

            val accountId = activeAccountId
            if (accountId != null && loginData.isLoggedIn(accountId)) {
                addAll(
                        when(sort) {
                            Sort.DATE -> gradesData.getGrades(accountId)[semester.value]
                                    ?.map { StandaloneGradeItem(it) }
                            Sort.SUBJECTS -> gradesData.getSubjects(accountId)[semester.value]
                                    ?.map { StandaloneSubjectItem(it) }
                        } ?: emptyList()
                )
            }
        }
        updateStatusSnackbar()
    }

    private fun updateStatusSnackbar() {
        val accountId = activeAccountId
        val syncResult = if (accountId != null) gradesData.getLastSyncResult(accountId) else null
        if (unbinder == null) return Log.w(LOG_TAG,
                "updateStatusSnackbar(): Can't show snackbar, no view available.")

        when (syncResult) {
            FAIL_UNKNOWN -> statusSnackbar = Snackbar.make(
                    baseView,
                    R.string.snackbar_grades_refresh_fail_unknown,
                    Snackbar.LENGTH_INDEFINITE
            ).apply {
                setAction(R.string.snackbar_action_grades_retry) { requestSyncWithLoading() }
                show()
            }
            FAIL_CONNECT -> statusSnackbar = Snackbar.make(
                    baseView,
                    R.string.snackbar_grades_refresh_fail_connect,
                    Snackbar.LENGTH_INDEFINITE
            ).apply {
                setAction(R.string.snackbar_action_grades_retry) { requestSyncWithLoading() }
                show()
            }
            FAIL_LOGIN -> statusSnackbar = Snackbar.make(
                    baseView,
                    R.string.snackbar_grades_refresh_fail_login,
                    Snackbar.LENGTH_INDEFINITE
            ).apply {
                setAction(R.string.snackbar_action_grades_logout) { logout() }
                show()
            }
            null -> statusSnackbar = Snackbar.make(
                    baseView,
                    R.string.snackbar_grades_fail_no_account,
                    Snackbar.LENGTH_INDEFINITE
            ).apply { show() }
            else -> statusSnackbar?.apply {
                dismiss()
                statusSnackbar = null
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_grades_menu, menu)

        menu.findItem(
                if (sort == Sort.DATE) R.id.action_sort_date
                else R.id.action_sort_subject
        ).isChecked = true

        menu.findItem(
                if (semester == Semester.FIRST) R.id.action_semester_first
                else R.id.action_semester_second
        ).isChecked = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item)) return true

        when (item.itemId) {
            R.id.action_sort_date -> {
                item.isChecked = true
                sort = Sort.DATE
            }
            R.id.action_sort_subject -> {
                item.isChecked = true
                sort = Sort.SUBJECTS
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
            R.id.action_log_out -> logout()
            else -> return false
        }
        return true
    }

    override fun onDestroyView() {
        recyclerManager = null
        unbinder?.unbind()
        unbinder = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        LocalBroadcast.unregisterReceiver(accountChangedReceiver)
        LocalBroadcast.unregisterReceiver(loginDataChangedReceiver)
        LocalBroadcast.unregisterReceiver(dataChangedReceiver)

        syncObserverHandle?.let {
            ContentResolver.removeStatusChangeListener(it)
            syncObserverHandle = null
        }

        adapter = null

        super.onDestroy()
    }

    override fun getTitle() = getText(R.string.action_show_grades)

    override fun getThemeId() = R.style.AppTheme_Grades

    enum class Sort {
        DATE, SUBJECTS
    }
}

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
import android.os.Bundle
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
import cz.anty.purkynka.grades.save.GradesSyncAdapter
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.LocalBroadcast
import eu.codetopic.utils.AndroidExtensions.edit
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import eu.codetopic.utils.ui.container.adapter.CustomItemAdapter
import eu.codetopic.utils.ui.container.recycler.Recycler
import eu.codetopic.utils.ui.container.recycler.utils.RecyclerItemClickListener.SimpleClickListener

/**
 * @author anty
 */
class GradesFragment : NavigationFragment(), TitleProvider, ThemeProvider {

    companion object {

        private const val LOG_TAG = "GradesFragment"
    }

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
    private var adapter: CustomItemAdapter<Grade>? = null

    private val gradesData = GradesData.instance
    private val loginData = gradesData.loginData
    private val activeAccountManager = ActiveAccountManager.instance

    private var activeAccount: Account? = null
    private var activeAccountId: String? = null

    private val dataChangedReceiver = broadcast { _, _ -> updateViews() }

    init {
        setHasOptionsMenu(true)
    }

    private fun showNoAccountSnack(message: CharSequence) {
        Snackbar.make(
                view ?: return Log.w(LOG_TAG, "showNoAccountSnack($message):" +
                        " Can't show snackbar, no view available."),
                message, Snackbar.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = CustomItemAdapter(context)
        LocalBroadcast.registerReceiver(
                dataChangedReceiver,
                intentFilter(GradesData.getter, ActiveAccountManager.getter)
        )
        // TODO: use observer to listen on synchronization state changes
        updateViews()
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val themedInflater = inflater.cloneInContext(
                ContextThemeWrapper(inflater.context, R.style.AppTheme_Grades))
        val baseView = themedInflater.inflate(R.layout.fragment_grades, container, false)
        unbinder = ButterKnife.bind(this, baseView)

        recyclerManager = Recycler.inflate().withSwipeToRefresh()
                .on(themedInflater, recyclerContainer, true)
                //.setEmptyImage() // TODO: add
                .setEmptyText("No grades") // TODO: to strings
                .setAdapter(adapter)
                .setOnRefreshListener { _ ->
                    GradesSyncAdapter.requestSync(
                            activeAccount ?:
                                    return@setOnRefreshListener showNoAccountSnack(
                                            "Can't request sync for you, no account available."),
                            Semester.AUTO) // TODO: option to change semester
                }
                .setItemTouchListener(object : SimpleClickListener() {
                    override fun onClick(view: View, position: Int) {
                        super.onClick(view, position) // TODO: implement
                        // TODO: show grade info and stats
                    }
                })

        updateViews()
        return baseView
    }

    @OnClick(R.id.but_login)
    fun onLoginClick() {
            loginData.login(
                    activeAccountId ?:
                            return showNoAccountSnack("Can't log you in, no account available."), // TODO: To strings
                    inputUsername.text.toString(),
                    inputPassword.text.toString()
            )
    }

    private fun updateViews() {
        val account = activeAccountManager.activeAccount
        val accountId = if (account == null) null else
            AccountsHelper.getAccountId(context, account)

        activeAccount = account
        activeAccountId = accountId
        if (accountId != null)
            Toast.makeText(context,
                    "Last synchronization state: ${gradesData.getLastSyncResult(accountId)}",
                    Toast.LENGTH_SHORT
            ).show()

        if (unbinder == null) return
        if (accountId != null && loginData.isLoggedIn(accountId)) {
            recyclerContainer.visibility = View.VISIBLE
            loginContainer.visibility = View.GONE

            val syncActive = ContentResolver.isSyncActive(account, GradesSyncAdapter.CONTENT_AUTHORITY)
            val syncPending = ContentResolver.isSyncPending(account, GradesSyncAdapter.CONTENT_AUTHORITY)
            if (!syncActive && !syncPending) recyclerManager?.setRefreshing(false)

            // TODO: create and here update synchronization status (went everything ok?)
            adapter?.edit {
                clear()
                addAll(gradesData.getGrades(accountId)[Semester.AUTO.value]) // TODO: option to change semester
            }
        } else {
            recyclerContainer.visibility = View.GONE
            loginContainer.visibility = View.VISIBLE

            if (accountId != null) inputUsername.setText(loginData.getUsername(accountId))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        recyclerManager = null
        unbinder?.unbind()
        unbinder = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        LocalBroadcast.unregisterReceiver(dataChangedReceiver)
        adapter = null
        super.onDestroy()
    }

    override fun getTitle() = getText(R.string.action_show_grades)

    override fun getThemeId() = R.style.AppTheme_Grades
}

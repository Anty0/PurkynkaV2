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

package cz.anty.purkynka.account.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.firebase.analytics.FirebaseAnalytics
import cz.anty.purkynka.R
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.utils.FBA_ACCOUNT_EDIT
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.CoordinatorLayoutModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.view.hideKeyboard
import kotlinx.android.synthetic.main.activity_edit_account.*
import org.jetbrains.anko.accountManager
import org.jetbrains.anko.design.longSnackbar

/**
 * @author anty
 */
class AccountEditActivity : ModularActivity(CoordinatorLayoutModule(), ToolbarModule(), BackButtonModule()) {

    companion object {

        const val KEY_ACCOUNT = "cz.anty.purkynka.accounts.ui.AccountEditActivity.KEY_ACCOUNT"
    }

    private var firebaseAnalytics: FirebaseAnalytics? = null

    private var accountManager: AccountManager? = null
    private var account: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED.
        setResult(RESULT_CANCELED)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        setContentView(R.layout.activity_edit_account)

        butSave.setOnClickListener(::save)

        accountManager = (this as Context).accountManager
        account = intent.getParcelableExtra(KEY_ACCOUNT)

        if (savedInstanceState == null) inAccountName.setText(account?.name)
    }

    fun save(v: View) {
        val accountManager = accountManager ?: return
        val account = account ?: return

        currentFocus?.hideKeyboard()

        val userName = inAccountName.text.toString()
                .trim().takeIf { it.isNotEmpty() } ?: run {
            longSnackbar(v, R.string.snackbar_invalid_account_name)
            return
        }

        val newAccount = Accounts.rename(this, accountManager, account, userName) ?: run {
            longSnackbar(v, R.string.snackbar_account_edit_failed)
            return
        }

        val intent = Intent()
                .putExtra(AccountManager.KEY_ACCOUNT_NAME, newAccount.name)
                .putExtra(AccountManager.KEY_ACCOUNT_TYPE, newAccount.type)

        setResult(RESULT_OK, intent)

        firebaseAnalytics?.logEvent(FBA_ACCOUNT_EDIT, null)

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        account = null
        accountManager = null

        firebaseAnalytics = null
    }
}
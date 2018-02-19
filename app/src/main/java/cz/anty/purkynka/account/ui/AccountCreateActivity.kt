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

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import cz.anty.purkynka.R
import cz.anty.purkynka.account.Accounts
import eu.codetopic.utils.accountManager
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.activity_create_account.*

/**
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class AccountCreateActivity : ModularActivity(ToolbarModule(), BackButtonModule()) {

    private lateinit var accountManager: AccountManager

    private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null

    var accountAuthenticatorResult: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        butCreate.setOnClickListener(::login)

        accountManager = (this as Context).accountManager
        accountAuthenticatorResponse =
                intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)

        accountAuthenticatorResponse?.onRequestContinued()
    }

    fun login(v: View) {
        val userName = inAccountName.text.toString()
                .trim().takeIf { it.isNotEmpty() } ?:
                run {
                    Snackbar.make(v, R.string.snackbar_invalid_account_name, Snackbar.LENGTH_LONG).show()
                    return
                }

        val account = Accounts.add(this, accountManager, userName)
        if (account != null) {
            val intent = Intent()
                    .putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name)
                    .putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type)

            accountAuthenticatorResult = intent.extras
            setResult(RESULT_OK, intent)
            finish()
            return
        }

        Snackbar.make(v, R.string.snackbar_login_failed, Snackbar.LENGTH_LONG).show()
    }

    override fun finish() {
        accountAuthenticatorResponse?.apply {
            // send the result bundle back if set, otherwise send an error.
            accountAuthenticatorResult
                    ?.also { onResult(it) }
                    ?: onError(AccountManager.ERROR_CODE_CANCELED, "canceled")

            accountAuthenticatorResponse = null
        }
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        accountAuthenticatorResponse = null
    }
}
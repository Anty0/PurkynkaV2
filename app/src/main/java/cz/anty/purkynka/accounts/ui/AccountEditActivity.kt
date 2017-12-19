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

package cz.anty.purkynka.accounts.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View

import cz.anty.purkynka.R
import cz.anty.purkynka.accounts.AccountsHelper
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.CoordinatorLayoutModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.activity_edit_account.*

/**
 * Created by anty on 10/15/17.
 *
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class AccountEditActivity : ModularActivity(ToolbarModule(), CoordinatorLayoutModule(), BackButtonModule()) {

    companion object {

        const val KEY_ACCOUNT = "cz.anty.purkynka.accounts.ui.AccountEditActivity.KEY_ACCOUNT"
    }

    private lateinit var accountManager: AccountManager
    private lateinit var account: Account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account)

        butSave.setOnClickListener(::save)

        accountManager = AccountManager.get(this)
        account = intent.getParcelableExtra(KEY_ACCOUNT)

        inAccountName.setText(account.name)
    }

    fun save(v: View) {
        val userName = inAccountName.text.toString()
                .trim().takeIf { it.isNotEmpty() } ?:
                run {
                    Snackbar.make(v, R.string.snackbar_invalid_account_name, Snackbar.LENGTH_LONG).show()
                    return
                }

        if (AccountsHelper.renameAccount(accountManager, this, account, userName)) {
            val intent = Intent()
                    .putExtra(AccountManager.KEY_ACCOUNT_NAME, userName)
                    .putExtra(AccountManager.KEY_ACCOUNT_TYPE, account.type)

            setResult(RESULT_OK, intent)
            finish()
            return
        }

        Snackbar.make(v, R.string.snackbar_edit_failed, Snackbar.LENGTH_LONG).show()
    }
}

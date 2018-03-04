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

package cz.anty.purkynka.account.service

import android.accounts.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.account.ui.AccountCreateActivity
import cz.anty.purkynka.account.ui.AccountEditActivity
import eu.codetopic.java.utils.log.Log
import org.jetbrains.anko.bundleOf
import java.util.*

/**
 * @author anty
 */
class AuthenticatorImpl(private val context: Context) : AbstractAccountAuthenticator(context) {

    companion object {

        private const val LOG_TAG = "AuthenticatorImpl"
    }

    override fun editProperties(response: AccountAuthenticatorResponse,
                                accountType: String): Bundle {
        Log.d(LOG_TAG, "editProperties(accountType=$accountType)")
        throw UnsupportedOperationException()
    }

    @Throws(NetworkErrorException::class)
    override fun addAccount(response: AccountAuthenticatorResponse, accountType: String,
                            authTokenType: String?, requiredFeatures: Array<String>?,
                            options: Bundle): Bundle {
        Log.d(LOG_TAG, "addAccount(accountType=$accountType, authTokenType=$authTokenType, " +
                "requiredFeatures=${Arrays.toString(requiredFeatures)}, " +
                "options=${Arrays.toString(options.keySet().toTypedArray())})")

        if (accountType != Accounts.ACCOUNT_TYPE)
            throw IllegalArgumentException("Unsupported accountType")

        return bundleOf(
                AccountManager.KEY_INTENT to
                        Intent(context, AccountCreateActivity::class.java)
                                .putExtra(
                                        AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                                        response
                                )
        )
    }

    override fun getAccountRemovalAllowed(response: AccountAuthenticatorResponse,
                                          account: Account): Bundle {
        Log.d(LOG_TAG, "getAccountRemovalAllowed(account=$account)")
        return bundleOf(
                AccountManager.KEY_BOOLEAN_RESULT to true
        )
    }

    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(response: AccountAuthenticatorResponse, account: Account,
                                    options: Bundle): Bundle {
        Log.d(LOG_TAG, "confirmCredentials(account=$account, " +
                "options=${Arrays.toString(options.keySet().toTypedArray())})")
        throw UnsupportedOperationException()
    }

    @Throws(NetworkErrorException::class)
    override fun getAuthToken(response: AccountAuthenticatorResponse, account: Account,
                              authTokenType: String?, options: Bundle): Bundle {
        Log.d(LOG_TAG, "getAuthToken(account=$account, authTokenType=$authTokenType, " +
                "options=${Arrays.toString(options.keySet().toTypedArray())})")
        throw UnsupportedOperationException()
    }

    override fun getAuthTokenLabel(authTokenType: String): String {
        Log.d(LOG_TAG, "getAuthTokenLabel(authTokenType=$authTokenType)")
        throw UnsupportedOperationException()
    }

    @Throws(NetworkErrorException::class)
    override fun updateCredentials(response: AccountAuthenticatorResponse, account: Account,
                                   authTokenType: String, options: Bundle): Bundle {
        Log.d(LOG_TAG, "updateCredentials(account=$account, authTokenType=$authTokenType, " +
                "options=${Arrays.toString(options.keySet().toTypedArray())})")

        // TODO: 10/12/17 check credentials from options
        return bundleOf(
                AccountManager.KEY_INTENT to
                        Intent(context, AccountEditActivity::class.java)
                                .putExtra(AccountEditActivity.KEY_ACCOUNT, account)
        )
    }

    @Throws(NetworkErrorException::class)
    override fun hasFeatures(response: AccountAuthenticatorResponse, account: Account,
                             features: Array<String>?): Bundle {
        Log.d(LOG_TAG, "hasFeatures(account=$account, " +
                "features=${features?.let { Arrays.toString(it) }})")

        return bundleOf(
                AccountManager.KEY_BOOLEAN_RESULT to (features?.isEmpty() != false)
        )
    }
}
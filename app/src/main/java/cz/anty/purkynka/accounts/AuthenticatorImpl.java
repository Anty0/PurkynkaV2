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

package cz.anty.purkynka.accounts;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Arrays;

import eu.codetopic.java.utils.log.Log;
import eu.codetopic.utils.bundle.BundleBuilder;

/**
 * Created by anty on 10/9/17.
 *
 * @author anty
 */
public class AuthenticatorImpl extends AbstractAccountAuthenticator {

    private static final String LOG_TAG = "AuthenticatorImpl";

    private final Context mContext;

    public AuthenticatorImpl(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        Log.d(LOG_TAG, String.format("editProperties: (%s)", accountType));
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
                             String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Log.d(LOG_TAG, String.format("addAccount: (%s, %s, %s, %s)", accountType, authTokenType,
                Arrays.toString(requiredFeatures), Arrays.toString(options.keySet().toArray())));
        return new BundleBuilder()
                .putParcelable(AccountManager.KEY_INTENT,
                        new Intent(mContext, AccountAddActivity.class)
                                .putExtra(AccountAddActivity.KEY_ACCOUNT_TYPE, accountType)
                                .putExtra(AccountAddActivity.KEY_AUTH_TYPE, authTokenType)
                                .putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response))
                .build();
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        Log.d(LOG_TAG, String.format("confirmCredentials: (%s, %s)", account, Arrays.toString(options.keySet().toArray())));
        throw new UnsupportedOperationException();
        /*return new BundleBuilder()
                .putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true) // TODO: implement activity
                .build();*/
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d(LOG_TAG, String.format("getAuthToken: (%s, %s, %s)", account, authTokenType, Arrays.toString(options.keySet().toArray())));
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        Log.d(LOG_TAG, String.format("getAuthTokenLabel: (%s)", authTokenType));
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d(LOG_TAG, String.format("updateCredentials: (%s, %s, %s)", account, authTokenType, Arrays.toString(options.keySet().toArray())));
        // TODO: 10/12/17 check credentials from options
        return new BundleBuilder()
                .putParcelable(AccountManager.KEY_INTENT, new Intent(mContext, AccountEditActivity.class))
                .build();
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        Log.d(LOG_TAG, String.format("hasFeatures: (%s, %s)", account, Arrays.toString(features)));
        return new BundleBuilder()
                .putBoolean(AccountManager.KEY_BOOLEAN_RESULT, features.length == 0)
                .build();
    }
}

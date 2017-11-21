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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import cz.anty.purkynka.R;
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule;
import eu.codetopic.utils.ui.activity.modular.module.CoordinatorLayoutModule;
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule;
import eu.codetopic.utils.ui.activity.modular.ModularActivity;

/**
 * Created by anty on 10/9/17.
 *
 * @author anty
 */
public class AccountAddActivity extends ModularActivity {

    public static final String KEY_ACCOUNT_TYPE =
            "cz.anty.purkynka.accounts.AccountAddActivity.KEY_ACCOUNT_TYPE";
    public static final String KEY_AUTH_TYPE =
            "cz.anty.purkynka.accounts.AccountAddActivity.KEY_AUTH_TOKEN_TYPE";

    private AccountManager mAccountManager = null;
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private String mAccountType = null;
    private Bundle mResultBundle = null;

    private Unbinder mUnbinder = null;

    @BindView(R.id.edit_account_name)
    public TextInputEditText mAccountNameEditText;

    public AccountAddActivity() {
        super(new ToolbarModule(), new CoordinatorLayoutModule(), new BackButtonModule());
    }

    public final void setAccountAuthenticatorResult(Bundle result) {
        mResultBundle = result;
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_account);
        mUnbinder = ButterKnife.bind(this);

        mAccountManager = AccountManager.get(this);
        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        mAccountType = getIntent().getStringExtra(KEY_ACCOUNT_TYPE);

        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }
    }

    @OnClick(R.id.but_login)
    public void login(View v) {
        final String userName = mAccountNameEditText.getText().toString().trim();
        if (userName.isEmpty()) {
            Snackbar.make(v, R.string.snackbar_invalid_account_name, Snackbar.LENGTH_LONG).show();
            return;
        }

        final Account account = new Account(userName, mAccountType);
        if (AccountsHelper.addAccountExplicitly(mAccountManager, account)) {
            final Intent intent = new Intent()
                    .putExtra(AccountManager.KEY_ACCOUNT_NAME, userName)
                    .putExtra(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);

            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);
            finish();
            return;
        }
        Snackbar.make(v, R.string.snackbar_login_failed, Snackbar.LENGTH_LONG).show();
    }

    public void finish() {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED,
                        "canceled");
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUnbinder.unbind();
        mUnbinder = null;

        mAccountAuthenticatorResponse = null;
        mAccountManager = null;
    }
}

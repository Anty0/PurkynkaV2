/*
 * Copyright 2017 Jiří Kuchyňka (Anty)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package cz.anty.purkynka.accounts;

import android.accounts.Account;
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
import eu.codetopic.utils.ui.activity.modular.ModularActivity;
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule;
import eu.codetopic.utils.ui.activity.modular.module.CoordinatorLayoutModule;
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule;

/**
 * Created by anty on 10/15/17.
 *
 * @author anty
 */
public class AccountEditActivity extends ModularActivity {

    public static final String KEY_ACCOUNT =
            "cz.anty.purkynka.accounts.AccountEditActivity.KEY_ACCOUNT";

    private AccountManager mAccountManager = null;
    private Account mAccount = null;

    private Unbinder mUnbinder = null;

    @BindView(R.id.edit_account_name)
    public TextInputEditText mAccountNameEditText;

    public AccountEditActivity() {
        super(new ToolbarModule(), new CoordinatorLayoutModule(), new BackButtonModule());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account);
        mUnbinder = ButterKnife.bind(this);

        mAccountManager = AccountManager.get(this);
        mAccount = getIntent().getParcelableExtra(KEY_ACCOUNT);

        mAccountNameEditText.setText(mAccount.name);
    }


    @OnClick(R.id.but_save)
    public void save(View v) {
        final String userName = mAccountNameEditText.getText().toString().trim();
        if (userName.isEmpty()) {
            Snackbar.make(v, R.string.snackbar_invalid_account_name, Snackbar.LENGTH_LONG).show();
            return;
        }

        if (AccountsHelper.renameAccount(mAccountManager, mAccount, userName)) {
            final Intent intent = new Intent()
                    .putExtra(AccountManager.KEY_ACCOUNT_NAME, userName)
                    .putExtra(AccountManager.KEY_ACCOUNT_TYPE, mAccount.type);

            setResult(RESULT_OK, intent);
            finish();
            return;
        }
        Snackbar.make(v, R.string.snackbar_edit_failed, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUnbinder.unbind();
        mUnbinder = null;

        mAccountManager = null;
    }
}

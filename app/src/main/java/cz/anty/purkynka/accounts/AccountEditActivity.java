/*
 * ApplicationPurkynka
 * Copyright (C)  2017  anty
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

        final Intent intent = new Intent()
                .putExtra(AccountManager.KEY_ACCOUNT_NAME, userName)
                .putExtra(AccountManager.KEY_ACCOUNT_TYPE, mAccount.type);
        if (AccountsHelper.renameAccount(mAccountManager, mAccount, userName)) {
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

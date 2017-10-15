package cz.anty.purkynka.accounts;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
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
public class AuthenticatorActivity extends ModularActivity {

    public static final String KEY_ACCOUNT_TYPE =
            "cz.anty.purkynka.accounts.AuthenticatorActivity.KEY_ACCOUNT_TYPE";
    public static final String KEY_AUTH_TYPE =
            "cz.anty.purkynka.accounts.AuthenticatorActivity.KEY_AUTH_TOKEN_TYPE";

    private AccountManager mAccountManager = null;
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;

    private Unbinder mUnbinder = null;

    @BindView(R.id.edit_account_name)
    public TextInputEditText mAccountNameEditText;

    public AuthenticatorActivity() {
        super(new ToolbarModule(), new CoordinatorLayoutModule(), new BackButtonModule());
    }

    public final void setAccountAuthenticatorResult(Bundle result) {
        mResultBundle = result;
    }

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_new_account);
        mUnbinder = ButterKnife.bind(this);

        mAccountManager = AccountManager.get(this);
        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

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

        final Bundle extras = getIntent().getExtras();
        final String accountType = extras.getString(KEY_ACCOUNT_TYPE);
        final Intent intent = new Intent()
                .putExtra(AccountManager.KEY_ACCOUNT_NAME, userName)
                .putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        final Account account = new Account(userName, accountType);
        mAccountManager.addAccountExplicitly(account, null, new Bundle());
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
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

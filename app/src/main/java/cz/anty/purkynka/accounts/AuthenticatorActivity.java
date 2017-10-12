package cz.anty.purkynka.accounts;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;

import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import cz.anty.purkynka.R;
import eu.codetopic.utils.ui.activity.BackButtonModule;
import eu.codetopic.utils.ui.activity.ToolbarModule;
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

    public AuthenticatorActivity() {
        super(new ToolbarModule(), new BackButtonModule());
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

    /*private void explicitLoginExample() {
        final Account account = new Account(mUsername, your_account_type);
        mAccountManager.addAccountExplicitly(account, mPassword, null);
    }*/

    @OnClick(R.id.but_login)
    public void login() {
        final Bundle extras = getIntent().getExtras();
        final String userName = UUID.randomUUID().toString();
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

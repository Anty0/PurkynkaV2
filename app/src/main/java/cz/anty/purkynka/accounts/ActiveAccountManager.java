package cz.anty.purkynka.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;

import eu.codetopic.java.utils.Objects;
import eu.codetopic.utils.data.getter.DataGetter;
import eu.codetopic.utils.data.preferences.PreferencesGetterAbs;
import eu.codetopic.utils.data.preferences.SharedPreferencesData;

import static cz.anty.purkynka.PrefNames.*;

/**
 * Created by anty on 10/13/17.
 *
 * @author anty
 */
public class ActiveAccountManager extends SharedPreferencesData {

    public static final DataGetter<ActiveAccountManager> getter = new Getter();

    private static final String LOG_TAG = "MainData";
    private static final int SAVE_VERSION = 0;

    private static ActiveAccountManager mInstance = null;

    private final AccountManager mAccountManager;

    private ActiveAccountManager(Context context) {
        super(context, FILE_NAME_ACTIVE_ACCOUNT_DATA, SAVE_VERSION);
        mAccountManager = AccountManager.get(getContext());
    }

    public static void initialize(Context context) {
        if (mInstance != null) throw new IllegalStateException(LOG_TAG + " is still initialized");
        mInstance = new ActiveAccountManager(context);
        mInstance.init();
    }

    public Account[] getAvailableAccounts() {
        return mAccountManager.getAccountsByType(AccountsHelper.ACCOUNT_TYPE);
    }

    public void setActiveAccount(Account account) {
        setActiveAccount(account.name);
    }

    public void setActiveAccount(String accountName) {
        edit().putString(ACTIVE_ACCOUNT_NAME, accountName).apply();
    }

    @Nullable
    public Account getActiveAccount() {
        String name = getPreferences().getString(ACTIVE_ACCOUNT_NAME, null);
        Account[] avAccounts = getAvailableAccounts();

        if (name != null) {
            for (Account avAccount : avAccounts) {
                if (Objects.equals(avAccount.name, name)) return avAccount;
            }
            if (Build.VERSION.SDK_INT >= 21) {
                for (Account avAccount : avAccounts) {
                    String previousName = mAccountManager.getPreviousName(avAccount);
                    if (Objects.equals(previousName, name)) {
                        setActiveAccount(avAccount.name);
                        return avAccount;
                    }
                }
            }
        }

        if (avAccounts.length > 0) {
            setActiveAccount(avAccounts[0]);
            return avAccounts[0];
        }
        return null;
    }

    private static final class Getter extends PreferencesGetterAbs<ActiveAccountManager> {

        @Override
        public ActiveAccountManager get() {
            return mInstance;
        }

        @Override
        public Class<ActiveAccountManager> getDataClass() {
            return ActiveAccountManager.class;
        }
    }
}

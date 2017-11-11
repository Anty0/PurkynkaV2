package cz.anty.purkynka.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.Nullable;

import eu.codetopic.java.utils.Objects;
import eu.codetopic.utils.data.getter.DataGetter;
import eu.codetopic.utils.data.preferences.VersionedPreferencesData;
import eu.codetopic.utils.data.preferences.provider.BasicSharedPreferencesProvider;
import eu.codetopic.utils.data.preferences.support.PreferencesGetterAbs;

import static cz.anty.purkynka.PrefNames.*;

/**
 * Created by anty on 10/13/17.
 *
 * @author anty
 */
public final class ActiveAccountManager extends VersionedPreferencesData<SharedPreferences> {

    public static final DataGetter<ActiveAccountManager> getter = new Getter();

    private static final String LOG_TAG = "MainData";
    private static final int SAVE_VERSION = 0;

    private static ActiveAccountManager mInstance = null;

    private final AccountManager mAccountManager;

    private ActiveAccountManager(Context context) {
        super(context, new BasicSharedPreferencesProvider(context,
                FILE_NAME_ACTIVE_ACCOUNT_DATA, Context.MODE_PRIVATE), SAVE_VERSION);
        mAccountManager = AccountManager.get(getContext());
    }

    public static void initialize(Context context) {
        if (mInstance != null) throw new IllegalStateException(LOG_TAG + " is still initialized");
        mInstance = new ActiveAccountManager(context);
        mInstance.init();
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
        Account[] avAccounts = AccountsHelper.getAllAccounts(mAccountManager);

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

    @Nullable
    public String getActiveAccountId() {
        Account activeAccount = getActiveAccount();
        return activeAccount == null ? null
                : AccountsHelper.getAccountId(getContext(), activeAccount);
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

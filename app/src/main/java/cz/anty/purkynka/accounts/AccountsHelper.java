package cz.anty.purkynka.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;

import java.util.UUID;

import eu.codetopic.utils.BundleBuilder;

/**
 * Created by anty on 10/11/17.
 *
 * @author anty
 */
public final class AccountsHelper {

    private AccountsHelper() {}

    public static final String ACCOUNT_TYPE = "cz.anty.purkynka.account";
    private static final String KEY_ACCOUNT_ID = "cz.anty.purkynka.account.ACCOUNT_ID";

    public static boolean initDefaultAccount(Context ctx) {
        AccountManager accountMan = AccountManager.get(ctx);
        if (accountMan.getAccountsByType(ACCOUNT_TYPE).length == 0) {
            Account defaultAccount = new Account("User", ACCOUNT_TYPE);
            return addAccountExplicitly(accountMan, defaultAccount);
        }
        return false;
    }

    public static Account[] getAllAccounts(Context ctx) {
        return getAllAccounts(AccountManager.get(ctx));
    }

    public static Account[] getAllAccounts(AccountManager accountManager) {
        return accountManager.getAccountsByType(AccountsHelper.ACCOUNT_TYPE);
    }

    public static boolean addAccountExplicitly(Context ctx, Account account) {
        return addAccountExplicitly(AccountManager.get(ctx), account);
    }

    public static boolean addAccountExplicitly(AccountManager accountManager, Account account) {
        return addAccountExplicitly(accountManager, account, UUID.randomUUID().toString());
    }

    private static boolean addAccountExplicitly(AccountManager accountManager, Account account, String accountId) {
        if (accountManager.addAccountExplicitly(account, null,
                new BundleBuilder()
                        .putString(KEY_ACCOUNT_ID, accountId)
                        .build())) {
            // TODO: 10/11/17 Enable sync for account
            // Inform the system that this account supports sync
            ///ContentResolver.setIsSyncable(defaultAccount, CONTENT_AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync when the network is up
            ///ContentResolver.setSyncAutomatically(defaultAccount, CONTENT_AUTHORITY, true);
            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ///ContentResolver.addPeriodicSync(defaultAccount, CONTENT_AUTHORITY, new Bundle(), SYNC_FREQUENCY);
            return true;
        }
        return false;
    }

    public static boolean renameAccount(Context ctx, Account account, String newName) {
        return renameAccount(AccountManager.get(ctx), account, newName);
    }

    public static boolean renameAccount(AccountManager accountManager, Account account, String newName) {
        /*if (Build.VERSION.SDK_INT >= 21) {
            accountManager.renameAccount(account, newName, null, null);
            return true;
        } else {*/
        String accountId = accountManager.getUserData(account, AccountsHelper.KEY_ACCOUNT_ID);
        if (addAccountExplicitly(accountManager, new Account(newName, account.type), accountId)) {
            accountManager.removeAccount(account, null, null);
            return true;
        }
        return false;
        //}
    }

    public static String getAccountId(Context ctx, Account account) {
        return getAccountId(AccountManager.get(ctx), account);
    }

    public static String getAccountId(AccountManager accountManager, Account account) {
        return accountManager.getUserData(account, KEY_ACCOUNT_ID);
    }

    public static void triggerSync(Account account, String contentAuthority) {
        ContentResolver.requestSync(account, contentAuthority, new BundleBuilder()
                .putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                .putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                .build());
    }
}

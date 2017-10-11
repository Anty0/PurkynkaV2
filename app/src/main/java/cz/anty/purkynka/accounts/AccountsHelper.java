package cz.anty.purkynka.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;

import eu.codetopic.utils.BundleBuilder;

/**
 * Created by anty on 10/11/17.
 *
 * @author anty
 */
public final class AccountsHelper {

    private AccountsHelper() {}

    public static final String ACCOUNT_TYPE = "cz.anty.purkynka.account";

    public static void initDefaultAccount(Context ctx) {
        AccountManager accountMan = AccountManager.get(ctx);
        if (accountMan.getAccountsByType(ACCOUNT_TYPE).length == 0) {
            Account defaultAccount = new Account("User", ACCOUNT_TYPE);
            if (accountMan.addAccountExplicitly(defaultAccount, null, null)) {
                // TODO: 10/11/17 Enable sync for account
                // Inform the system that this account supports sync
                ///ContentResolver.setIsSyncable(defaultAccount, CONTENT_AUTHORITY, 1);
                // Inform the system that this account is eligible for auto sync when the network is up
                ///ContentResolver.setSyncAutomatically(defaultAccount, CONTENT_AUTHORITY, true);
                // Recommend a schedule for automatic synchronization. The system may modify this based
                // on other scheduled syncs and network utilization.
                ///ContentResolver.addPeriodicSync(defaultAccount, CONTENT_AUTHORITY, new Bundle(), SYNC_FREQUENCY);
            }
        }
    }

    public static void TriggerSync(Account account, String contentAuthority) {
        ContentResolver.requestSync(account, contentAuthority, new BundleBuilder()
                .putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                .putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                .build());
    }
}

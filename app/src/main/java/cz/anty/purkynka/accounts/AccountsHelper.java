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
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import java.util.UUID;

import cz.anty.purkynka.accounts.notify.AccountNotificationChannel;
import eu.codetopic.utils.bundle.BundleBuilder;

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
            return addAccountExplicitly(accountMan, ctx, defaultAccount);
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
        return addAccountExplicitly(AccountManager.get(ctx), ctx, account);
    }

    public static boolean addAccountExplicitly(AccountManager accountManager,
                                               Context ctx, Account account) {
        return addAccountExplicitly(accountManager, ctx, account, UUID.randomUUID().toString());
    }

    private static boolean addAccountExplicitly(AccountManager accountManager, Context ctx,
                                                Account account, String accountId) {
        if (accountManager.addAccountExplicitly(account, null,
                new BundleBuilder()
                        .putString(KEY_ACCOUNT_ID, accountId)
                        .build())) {

            // FIXME: listen on accounts changes and do all refreshing there
            AccountNotificationChannel.Companion.refresh(ctx);
            return true;
        }
        return false;
    }

    public static boolean renameAccount(Context ctx, Account account, String newName) {
        return renameAccount(AccountManager.get(ctx), ctx, account, newName);
    }

    public static boolean renameAccount(AccountManager accountManager, Context ctx,
                                        Account account, String newName) {
        /*if (Build.VERSION.SDK_INT >= 21) {
            accountManager.renameAccount(account, newName, null, null);
            return true;
        } else {*/
        String accountId = accountManager.getUserData(account, AccountsHelper.KEY_ACCOUNT_ID);
        if (addAccountExplicitly(accountManager, ctx, new Account(newName, account.type), accountId)) {
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

    public static void enableSyncOf(Account account, String contentAuthority, long syncFrequency) {
        ContentResolver.setIsSyncable(account, contentAuthority, 1);
        ContentResolver.setSyncAutomatically(account, contentAuthority, true);
        ContentResolver.addPeriodicSync(account, contentAuthority, new Bundle(), syncFrequency);
    }

    public static void disableSyncOf(Account account, String contentAuthority) {
        ContentResolver.setIsSyncable(account, contentAuthority, 0);
    }
}

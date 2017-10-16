package cz.anty.purkynka.accounts

import android.accounts.AccountManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Created by anty on 10/9/17.
 * @author anty
 */
class AuthenticatorService : Service() {

    private var mAuthenticator: AuthenticatorImpl? = null

    override fun onCreate() {
        super.onCreate()
        mAuthenticator = AuthenticatorImpl(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == AccountManager.ACTION_AUTHENTICATOR_INTENT) {
            return mAuthenticator?.iBinder
        }
        return null
    }

    override fun onDestroy() {
        mAuthenticator = null
        super.onDestroy()
    }
}
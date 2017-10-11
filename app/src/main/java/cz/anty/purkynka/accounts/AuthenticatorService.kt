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

    private var authenticator: Authenticator? = null

    override fun onCreate() {
        super.onCreate()
        authenticator = Authenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == AccountManager.ACTION_AUTHENTICATOR_INTENT) {
            return authenticator?.iBinder
        }
        return null
    }

    override fun onDestroy() {
        authenticator = null
        super.onDestroy()
    }
}
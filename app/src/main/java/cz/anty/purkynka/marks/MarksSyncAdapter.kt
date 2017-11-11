package cz.anty.purkynka.marks

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import cz.anty.purkynka.Constants
import eu.codetopic.java.utils.log.Log

/**
 * @author anty
 */
class MarksSyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, false, true) {

    companion object {
        private const val LOG_TAG = "MarksSyncAdapter"

        const val CONTENT_AUTHORITY = MarksProvider.AUTHORITY
        const val SYNC_FREQUENCY: Long = Constants.SYNC_FREQUENCY_MARKS
    }

    override fun onPerformSync(account: Account, extras: Bundle, authority: String,
                               provider: ContentProviderClient, syncResult: SyncResult) {
        Log.d(LOG_TAG, "onPerformSync")

        if (authority != CONTENT_AUTHORITY) return


    }
}
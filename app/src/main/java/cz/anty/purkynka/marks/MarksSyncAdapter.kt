package cz.anty.purkynka.marks

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import eu.codetopic.java.utils.log.Log

/**
 * Created by anty on 10/16/17.
 * @author anty
 */
class MarksSyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, false, true) {

    companion object {
        private const val LOG_TAG = "MarksSyncAdapter"

        public const val CONTENT_AUTHORITY = "cz.anty.purkynka.marks.data"
        public const val SYNC_FREQUENCY: Long = 60 * 15 // 15 minutes in seconds
    }

    override fun onPerformSync(account: Account?, extras: Bundle?, authority: String?, provider: ContentProviderClient?, syncResult: SyncResult?) {
        Log.d(LOG_TAG, "onPerformSync")
        TODO("not implemented")
    }
}
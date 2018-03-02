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

package cz.anty.purkynka.grades.sync

import android.accounts.Account
import android.content.*
import android.os.Bundle
import android.support.annotation.WorkerThread
import cz.anty.purkynka.utils.*
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.account.Syncs
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.load.GradesFetcher
import cz.anty.purkynka.grades.load.GradesParser
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel
import cz.anty.purkynka.grades.receiver.UpdateGradesSyncReceiver
import cz.anty.purkynka.grades.save.*
import cz.anty.purkynka.grades.save.GradesData.SyncResult.*
import cz.anty.purkynka.grades.widget.GradesWidgetProvider
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.putKSerializableExtra
import eu.codetopic.utils.broadcast.BroadcastsConnector
import eu.codetopic.utils.bundle.BundleSerializer
import kotlinx.serialization.list
import org.jetbrains.anko.bundleOf
import java.io.IOException

/**
 * @author anty
 */
class GradesSyncAdapter(context: Context) :
        AbstractThreadedSyncAdapter(context, false, false) {

    companion object {

        private const val LOG_TAG = "GradesSyncAdapter"

        const val CONTENT_AUTHORITY = GradesDataProvider.AUTHORITY
        const val SYNC_FREQUENCY = SYNC_FREQUENCY_GRADES

        const val EXTRA_SEMESTER = "cz.anty.purkynka.grades.sync.$LOG_TAG.EXTRA_SEMESTER"

        fun listenForChanges(context: Context) {
            BroadcastsConnector.connect(
                    GradesLoginData.instance.broadcastActionChanged,
                    BroadcastsConnector.Connection(
                            BroadcastsConnector.BroadcastTargetingType.GLOBAL,
                            UpdateGradesSyncReceiver.getIntent(context)
                    )
            )
            context.sendBroadcast(UpdateGradesSyncReceiver.getIntent(context))
        }

        fun updateSyncable(context: Context) {
            Syncs.updateAllAccountsBasedEnabled(
                    context = context,
                    loginData = GradesLoginData.loginData,
                    contentAuthority = CONTENT_AUTHORITY,
                    syncFrequency = SYNC_FREQUENCY
            )
        }

        fun updateSyncableOf(accountId: String, account: Account) {
            Syncs.updateAccountBasedEnabled(
                    accountId = accountId,
                    account = account,
                    loginData = GradesLoginData.loginData,
                    contentAuthority = CONTENT_AUTHORITY,
                    syncFrequency = SYNC_FREQUENCY
            )
        }

        fun requestSync(account: Account, semester: Semester = Semester.AUTO) {
            Log.d(LOG_TAG, "requestSync(account=$account, semester=$semester)")

            Syncs.trigger(
                    account = account,
                    contentAuthority = CONTENT_AUTHORITY,
                    extras = bundleOf(
                            EXTRA_SEMESTER to semester.toString()
                    )
            )
        }
    }

    override fun onPerformSync(account: Account, extras: Bundle, authority: String,
                               provider: ContentProviderClient, syncResult: SyncResult) {
        Log.d(LOG_TAG, "onPerformSync(account=(type=${account.type}," +
                " name=${account.name}), authority=$authority)")

        if (authority != CONTENT_AUTHORITY) return

        try {
            val semester = if (extras.containsKey(EXTRA_SEMESTER)) {
                try {
                    Semester.valueOf(extras.getString(EXTRA_SEMESTER)).stableSemester
                } catch (e: Exception) {
                    if (e is InterruptedException) throw e
                    Log.e(LOG_TAG, "onPerformSync() -> Failed to parse semester parameter", e)
                    Semester.AUTO.stableSemester
                }
            } else Semester.AUTO.stableSemester

            try {
                GradesSyncer.performSync(
                        context = context,
                        account = account,
                        semester = semester,
                        firstSync = false,
                        syncResult = syncResult
                )
            } catch (_: Exception) {
                // Exception handling is done in GradesSyncer
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to start sync of grades", e)

            syncResult.stats.numIoExceptions++
        }
    }
}
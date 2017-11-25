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

package cz.anty.purkynka.grades.save

import android.accounts.Account
import android.accounts.AccountManager
import android.content.*
import android.os.Bundle
import cz.anty.purkynka.Constants
import cz.anty.purkynka.accounts.AccountsHelper
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.load.GradesFetcher
import cz.anty.purkynka.grades.load.GradesParser
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.BundleBuilder
import eu.codetopic.utils.LocalBroadcast
import java.io.IOException

/**
 * @author anty
 */
class GradesSyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, false, false) {

    companion object {

        private const val LOG_TAG = "GradesSyncAdapter"

        const val CONTENT_AUTHORITY = GradesProvider.AUTHORITY
        const val SYNC_FREQUENCY = Constants.SYNC_FREQUENCY_GRADES

        const val EXTRA_SEMESTER = "cz.anty.purkynka.grades.save.$LOG_TAG.EXTRA_SEMESTER"

        private val loginDataChangedReceiver = broadcast { context, _ ->
            val loginData = GradesData.instance.loginData
            val accountManager = AccountManager.get(context)

            AccountsHelper.getAllAccounts(accountManager).forEach {
                with(AccountsHelper.getAccountId(accountManager, it)) {
                    val loggedIn = loginData.isLoggedIn(this)
                    val syncable = ContentResolver.getIsSyncable(it, CONTENT_AUTHORITY) > 0
                    if (loggedIn == syncable) return@with

                    ContentResolver.setIsSyncable(it, CONTENT_AUTHORITY, if (loggedIn) 1 else 0)
                    ContentResolver.setSyncAutomatically(it, CONTENT_AUTHORITY, true)
                    ContentResolver.addPeriodicSync(it, CONTENT_AUTHORITY, Bundle(), SYNC_FREQUENCY)
                }
            }
        }

        fun init(context: Context) {
            LocalBroadcast.registerReceiver(loginDataChangedReceiver, intentFilter(GradesData.getter))
            loginDataChangedReceiver.onReceive(context, null)
        }

        fun requestSync(account: Account, semester: Semester = Semester.AUTO) {
            ContentResolver.requestSync(account, CONTENT_AUTHORITY, BundleBuilder()
                    .putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                    .putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                    .putSerializable(EXTRA_SEMESTER, semester)
                    .build())
        }
    }

    override fun onPerformSync(account: Account, extras: Bundle, authority: String,
                               provider: ContentProviderClient, syncResult: SyncResult) {
        Log.d(LOG_TAG, "onPerformSync(" +
                "account=(type=${account.type}, name=${account.name}), " +
                "authority=$authority)")

        if (authority != CONTENT_AUTHORITY) return

        val semester = (extras.getSerializable(EXTRA_SEMESTER) ?: Semester.AUTO) as Semester

        val data = GradesData.instance
        val loginData = data.loginData

        val accountId = AccountsHelper.getAccountId(context, account)

        data.setLastSyncResult(accountId, GradesData.SyncResult.SYNCING)

        try {
            if (!loginData.isLoggedIn(accountId))
                throw WrongLoginDataException("User is not logged in")

            val cookies = GradesFetcher.login(
                    loginData.getUsername(accountId),
                    loginData.getPassword(accountId)
            )

            if (!GradesFetcher.isLoggedIn(cookies))
                throw WrongLoginDataException("Failed to login user with provided credentials")

            val gradesHtml = GradesFetcher.getGradesElements(cookies, semester)
            val grades = GradesParser.parseGrades(gradesHtml)

            val gradesMap = data.getGrades(accountId)
            gradesMap[semester.value]?.apply {
                // TODO: check for differences and show notification
                clear()
                addAll(grades)
            }
            data.setGrades(accountId, gradesMap)

            data.setLastSyncResult(accountId, GradesData.SyncResult.SUCCESS)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to refresh grades", e)

            data.setLastSyncResult(accountId, when (e) {
                is WrongLoginDataException -> GradesData.SyncResult.FAIL_LOGIN
                is IOException -> GradesData.SyncResult.FAIL_CONNECT
                else -> GradesData.SyncResult.FAIL_UNKNOWN
            })
        }
    }
}
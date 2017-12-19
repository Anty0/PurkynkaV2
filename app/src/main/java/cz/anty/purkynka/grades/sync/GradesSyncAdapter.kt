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
import android.accounts.AccountManager
import android.content.*
import android.os.Bundle
import cz.anty.purkynka.Constants
import cz.anty.purkynka.accounts.AccountsHelper
import cz.anty.purkynka.accounts.notify.AccountNotificationChannel
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.load.GradesFetcher
import cz.anty.purkynka.grades.load.GradesParser
import cz.anty.purkynka.grades.notify.GradesChangesNotificationGroup
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesData.SyncResult.*
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesProvider
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.bundle.BundleBuilder
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.notifications.manager.NotificationsManager
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

        private val loginDataChangedReceiver = broadcast { context, intent ->
            Log.d(LOG_TAG, "loginDataChanged(intent=$intent)")

            val loginData = GradesLoginData.loginData
            val accountManager = AccountManager.get(context)

            AccountsHelper.getAllAccounts(accountManager).forEach {
                with(AccountsHelper.getAccountId(accountManager, it)) {
                    val loggedIn = loginData.isLoggedIn(this)
                    val syncable = ContentResolver.getIsSyncable(it, CONTENT_AUTHORITY) > 0
                    if (loggedIn == syncable) return@with

                    Log.d(LOG_TAG, "loginDataChanged() -> " +
                            "differenceFound(loggedIn=$loggedIn, syncable=$syncable, account=$it)")

                    ContentResolver.setIsSyncable(it, CONTENT_AUTHORITY, if (loggedIn) 1 else 0)
                    ContentResolver.setSyncAutomatically(it, CONTENT_AUTHORITY, loggedIn)
                    if (loggedIn) ContentResolver.addPeriodicSync(it, CONTENT_AUTHORITY, Bundle(), SYNC_FREQUENCY)
                    else ContentResolver.removePeriodicSync(it, CONTENT_AUTHORITY, Bundle())
                    requestSync(it)
                }
            }
        }

        fun init(context: Context) {
            LocalBroadcast.registerReceiver(loginDataChangedReceiver, intentFilter(GradesLoginData.getter))
            loginDataChangedReceiver.onReceive(context, null)
        }

        fun requestSync(account: Account, semester: Semester = Semester.AUTO) {
            Log.d(LOG_TAG, "requestSync(account=$account, semester=$semester)")
            ContentResolver.requestSync(account, CONTENT_AUTHORITY, BundleBuilder()
                    .putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                    .putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                    .putString(EXTRA_SEMESTER, semester.toString())
                    .build())
        }
    }

    override fun onPerformSync(account: Account, extras: Bundle, authority: String,
                               provider: ContentProviderClient, syncResult: SyncResult) {
        Log.d(LOG_TAG, "onPerformSync(" +
                "account=(type=${account.type}, name=${account.name}), " +
                "authority=$authority)")

        if (authority != CONTENT_AUTHORITY) return

        val data = GradesData.instance
        val loginData = GradesLoginData.loginData

        val accountId = AccountsHelper.getAccountId(context, account)

        try {
            val semester = if (extras.containsKey(EXTRA_SEMESTER)) {
                try {
                    Semester.valueOf(extras.getString(EXTRA_SEMESTER)).stableSemester
                } catch (e: Exception) {
                    Log.w(LOG_TAG, e)
                    Semester.AUTO
                }
            } else Semester.AUTO

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

            val gradesMap = data.getGrades(accountId).toMutableMap()
            gradesMap.getOrElse(semester.value) { emptyList() }.let {
                checkForDiffs(accountId, it, grades)
            }
            gradesMap[semester.value] = grades
            data.setGrades(accountId, gradesMap)

            data.setLastSyncResult(accountId, SUCCESS)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to refresh grades", e)

            data.setLastSyncResult(accountId, when (e) {
                is WrongLoginDataException -> FAIL_LOGIN
                is IOException -> FAIL_CONNECT
                else -> FAIL_UNKNOWN
            })
        }
    }

    private fun checkForDiffs(accountId: String, oldGrades: List<Grade>, newGrades: List<Grade>) {
        val added = mutableListOf<Grade>()
        val modified = mutableListOf<Pair<Grade, Grade>>()
        //val removed = mutableListOf<Grade>()

        newGrades.forEach {
            val index = oldGrades.indexOf(it)
            if (index == -1) {
                added.add(it)
                return@forEach
            }

            val oldGrade = oldGrades[index]
            if (it differentTo oldGrade) modified.add(oldGrade to it)
        }

        //removed.addAll(oldGrades.filter { !newGrades.contains(it) })

        val allChanges = added.map {
            GradesChangesNotificationGroup.dataForNewGarde(it)
        } + modified.map {
            GradesChangesNotificationGroup.dataForModifiedGarde(it.first, it.second)
        }

        NotificationsManager.requestNotifyAll(context, GradesChangesNotificationGroup.ID,
                AccountNotificationChannel.idFor(accountId), allChanges)
        //GradesDataDifferences.instance.addNewDiffs(accountId, added, modified/*, removed*/)
    }
}
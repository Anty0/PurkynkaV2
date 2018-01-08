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
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.account.Syncs
import cz.anty.purkynka.account.notify.AccountNotifyChannel
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
import org.jetbrains.anko.bundleOf
import java.io.IOException

/**
 * @author anty
 */
class GradesSyncAdapter(context: Context) :
        AbstractThreadedSyncAdapter(context, false, false) {

    companion object {

        private const val LOG_TAG = "GradesSyncAdapter"

        const val CONTENT_AUTHORITY = GradesProvider.AUTHORITY
        const val SYNC_FREQUENCY = Constants.SYNC_FREQUENCY_GRADES

        const val EXTRA_SEMESTER = "cz.anty.purkynka.grades.save.$LOG_TAG.EXTRA_SEMESTER"

        private val loginDataChangedReceiver = broadcast { context, intent ->
            Log.d(LOG_TAG, "loginDataChanged(intent=$intent)")

            val loginData = GradesLoginData.loginData
            val accountManager = AccountManager.get(context)

            Accounts.getAllWIthIds(accountManager).forEach {
                val accountId = it.key
                val account = it.value

                val loggedIn = loginData.isLoggedIn(accountId)
                val syncable = ContentResolver.getIsSyncable(account, CONTENT_AUTHORITY)
                        .takeIf { it >= 0 }?.let { it > 0 }
                if (syncable != null && loggedIn == syncable) return@forEach

                Log.d(LOG_TAG, "loginDataChanged() -> differenceFound(loggedIn=$loggedIn," +
                        " syncable=${syncable ?: "Unknown"}, account=$it)")

                Syncs.updateEnabled(
                        loggedIn,
                        account,
                        CONTENT_AUTHORITY,
                        SYNC_FREQUENCY
                )
            }
        }

        fun init(context: Context) {
            LocalBroadcast.registerReceiver(loginDataChangedReceiver, intentFilter(GradesLoginData.getter))
            loginDataChangedReceiver.onReceive(context, null)
        }

        fun requestSync(account: Account, semester: Semester = Semester.AUTO) {
            Log.d(LOG_TAG, "requestSync(account=$account, " +
                    "semester=$semester)")

            Syncs.trigger(
                    account,
                    CONTENT_AUTHORITY,
                    bundleOf(
                            EXTRA_SEMESTER to semester.toString()
                    )
            )
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

        val accountId = Accounts.getId(context, account)

        try {
            val semester = if (extras.containsKey(EXTRA_SEMESTER)) {
                try {
                    Semester.valueOf(extras.getString(EXTRA_SEMESTER)).stableSemester
                } catch (e: Exception) {
                    Log.w(LOG_TAG, e)
                    Semester.AUTO
                }
            } else Semester.AUTO

            val firstSync = data.isFirstSync(accountId)

            if (!loginData.isLoggedIn(accountId))
                throw IllegalStateException("User is not logged in")

            val cookies = GradesFetcher.login(
                    loginData.getUsername(accountId),
                    loginData.getPassword(accountId)
            )

            if (!GradesFetcher.isLoggedIn(cookies))
                throw WrongLoginDataException("Failed to login user with provided credentials")

            val gradesHtml = GradesFetcher.getGradesElements(cookies, semester)
            val grades = GradesParser.parseGrades(gradesHtml)

            val gradesMap = data.getGrades(accountId).toMutableMap()
            gradesMap.takeIf { !firstSync }
                    ?.getOrElse(semester.value) { emptyList() }
                    ?.let {
                        checkForDiffs(accountId, it, grades)
                    }
            gradesMap[semester.value] = grades
            data.setGrades(accountId, gradesMap)

            data.notifyFirstSyncDone(accountId)
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
            GradesChangesNotificationGroup.dataForNewGrade(it)
        } + modified.map {
            GradesChangesNotificationGroup.dataForModifiedGrade(it.first, it.second)
        }

        NotificationsManager.requestNotifyAll(context, GradesChangesNotificationGroup.ID,
                AccountNotifyChannel.idFor(accountId), allChanges)
    }
}
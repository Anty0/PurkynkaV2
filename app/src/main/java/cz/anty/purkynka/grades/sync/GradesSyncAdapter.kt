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

        const val ACTION_NEW_GRADES_CHANGES =
                "cz.anty.purkynka.grades.sync.$LOG_TAG.NEW_GRADES_CHANGES"
        const val EXTRA_ACCOUNT_ID =
                "cz.anty.purkynka.grades.sync.$LOG_TAG.EXTRA_ACCOUNT_ID"
        const val EXTRA_GRADES_CHANGES =
                "cz.anty.purkynka.grades.sync.$LOG_TAG.EXTRA_GRADES_CHANGES"

        const val CONTENT_AUTHORITY = GradesDataProvider.AUTHORITY
        const val SYNC_FREQUENCY = SYNC_FREQUENCY_GRADES

        const val EXTRA_SEMESTER = "cz.anty.purkynka.grades.sync.$LOG_TAG.EXTRA_SEMESTER"

        fun init(context: Context) {
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

        val data = GradesData.instance
        val loginData = GradesLoginData.loginData
        val preferences = GradesPreferences.instance

        val accountId = Accounts.getId(context, account)

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

            val firstSync = data.isFirstSync(accountId)

            val semestersToFetch =
                    if (firstSync) arrayOf(Semester.FIRST, Semester.SECOND)
                    else arrayOf(semester)

            if (!loginData.isLoggedIn(accountId))
                throw IllegalStateException("User is not logged in")

            val (username, password) = loginData.getCredentials(accountId)

            if (username == null || password == null)
                throw IllegalStateException("Username or password is null")

            val cookies = GradesFetcher.login(username, password)

            if (!GradesFetcher.isLoggedIn(cookies))
                throw WrongLoginDataException("Failed to login user with provided credentials")

            val gradesMap = data.getGrades(accountId).toMutableMap()

            semestersToFetch.forEach {
                fetchGradesToMap(accountId, cookies, it, !firstSync, gradesMap, syncResult)
            }

            GradesFetcher.logout(cookies)

            data.setGrades(accountId, gradesMap)

            data.notifyFirstSyncDone(accountId)
            data.setLastSyncResult(accountId, SUCCESS)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to refresh grades", e)

            run setResult@ {
                data.setLastSyncResult(accountId, when (e) {
                    is WrongLoginDataException -> {
                        syncResult.stats.numAuthExceptions++
                        FAIL_LOGIN
                    }
                    is IOException -> {
                        syncResult.stats.numIoExceptions++
                        FAIL_CONNECT
                    }
                    is InterruptedException -> return@setResult
                    else -> {
                        syncResult.stats.numIoExceptions++
                        FAIL_UNKNOWN
                    }
                })
            }
        } finally {
            try {
                context.sendBroadcast(
                        GradesWidgetProvider.getUpdateIntent(
                                context,
                                GradesWidgetProvider.getAllWidgetIds(context)
                                        .filter { preferences.getAppWidgetAccountId(it) == accountId }
                                        .toIntArray()
                        )
                )
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to refresh grades widgets", e)
            }
        }
    }

    private fun fetchGradesToMap(accountId: String, cookies: Map<String, String>,
                                 semester: Semester, checkForChanges: Boolean,
                                 gradesMap: MutableGradesMap, syncResult: SyncResult) {
        val gradesHtml = GradesFetcher.getGradesElements(cookies, semester)
        val grades = GradesParser.parseGrades(gradesHtml, syncResult)

        gradesMap.takeIf { checkForChanges }
                ?.getOrElse(semester.value) { emptyList() }
                ?.let {
                    checkForDiffs(accountId, it, grades, syncResult)
                }
        gradesMap[semester.value] = grades
    }

    private fun checkForDiffs(accountId: String, oldGrades: List<Grade>, newGrades: List<Grade>,
                              syncResult: SyncResult) {
        val inserted = mutableListOf<Grade>()
        val updated = mutableListOf<Pair<Grade, Grade>>()
        val deleted = mutableListOf<Grade>()

        newGrades.forEach {
            val index = oldGrades.indexOf(it)
            if (index == -1) {
                inserted.add(it)
                return@forEach
            }

            val oldGrade = oldGrades[index]
            if (it differentTo oldGrade) updated.add(oldGrade to it)
        }

        deleted.addAll(oldGrades.filter { !newGrades.contains(it) })

        syncResult.stats.numInserts += inserted.size
        syncResult.stats.numUpdates += updated.size
        syncResult.stats.numDeletes += deleted.size

        val allChanges = inserted.map {
            GradesChangesNotifyChannel.dataForNewGrade(it)
        } + updated.map {
            GradesChangesNotifyChannel.dataForModifiedGrade(it.first, it.second)
        }

        if (allChanges.isEmpty()) return

        context.sendOrderedBroadcast(
                Intent(ACTION_NEW_GRADES_CHANGES)
                        .putExtra(EXTRA_ACCOUNT_ID, accountId)
                        .putKSerializableExtra(
                                name = EXTRA_GRADES_CHANGES,
                                value = allChanges,
                                saver = BundleSerializer.list
                        ),
                null
        )
    }
}
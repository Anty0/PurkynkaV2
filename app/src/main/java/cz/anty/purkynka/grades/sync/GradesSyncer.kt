/*
 * app
 * Copyright (C)   2018  anty
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
import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.support.annotation.WorkerThread
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.exceptions.WrongLoginDataException
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.load.GradesFetcher
import cz.anty.purkynka.grades.load.GradesParser
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesData.SyncResult.*
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesPreferences
import cz.anty.purkynka.grades.save.MutableGradesMap
import cz.anty.purkynka.grades.widget.GradesWidgetProvider
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.bundle.BundleSerializer
import eu.codetopic.utils.putKSerializableExtra
import kotlinx.serialization.list
import java.io.IOException

/**
 * @author anty
 */
object GradesSyncer {

    private const val LOG_TAG = "GradesSyncer"

    const val ACTION_NEW_GRADES_CHANGES =
            "cz.anty.purkynka.grades.sync.$LOG_TAG.NEW_GRADES_CHANGES"
    const val EXTRA_ACCOUNT_ID =
            "cz.anty.purkynka.grades.sync.$LOG_TAG.EXTRA_ACCOUNT_ID"
    const val EXTRA_GRADES_CHANGES =
            "cz.anty.purkynka.grades.sync.$LOG_TAG.EXTRA_GRADES_CHANGES"

    @WorkerThread
    fun performSync(
            context: Context,
            account: Account,
            semester: Semester = Semester.AUTO,
            credentials: Pair<String, String>? = null,
            firstSync: Boolean = false,
            syncResult: SyncResult? = null
    ) {
        var data: GradesData? = null
        var preferences: GradesPreferences? = null

        var accountId: String? = null

        try {
            data = GradesData.instance
            val loginData = GradesLoginData.loginData
            preferences = GradesPreferences.instance

            accountId = Accounts.getId(context, account)

            val semestersToFetch =
                    if (firstSync) arrayOf(Semester.FIRST, Semester.SECOND)
                    else arrayOf(semester)

            if (credentials == null && !loginData.isLoggedIn(accountId))
                throw IllegalStateException("User is not logged in")

            val (username, password) = credentials ?: loginData.getCredentials(accountId)

            if (username == null || password == null)
                throw IllegalStateException("Username or password is null")

            val cookies = GradesFetcher.login(username, password)

            if (!GradesFetcher.isLoggedIn(cookies))
                throw WrongLoginDataException("Failed to login user with provided credentials")

            val gradesMap = data.getGrades(accountId).toMutableMap()

            semestersToFetch.forEach {
                fetchGradesToMap(context, accountId, cookies, it, !firstSync, gradesMap, syncResult)
            }

            GradesFetcher.logout(cookies)

            data.setGrades(accountId, gradesMap)

            data.setLastSyncResult(accountId, SUCCESS)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to refresh grades", e)

            if (data != null && accountId != null) run setResult@ {
                data.setLastSyncResult(accountId, when (e) {
                    is WrongLoginDataException -> {
                        syncResult?.apply { stats.numIoExceptions++ }
                        FAIL_LOGIN
                    }
                    is IOException -> {
                        syncResult?.apply { stats.numIoExceptions++ }
                        FAIL_CONNECT
                    }
                    is InterruptedException -> return@setResult
                    else -> {
                        syncResult?.apply { stats.numIoExceptions++ }
                        FAIL_UNKNOWN
                    }
                })
            }

            throw e
        } finally {
            try {
                if (preferences != null) {
                    context.sendBroadcast(
                            GradesWidgetProvider.getUpdateIntent(
                                    context,
                                    GradesWidgetProvider.getAllWidgetIds(context)
                                            .filter { preferences.getAppWidgetAccountId(it) == accountId }
                                            .toIntArray()
                            )
                    )
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to refresh grades widgets", e)
            }
        }
    }

    @WorkerThread
    private fun fetchGradesToMap(
            context: Context,
            accountId: String,
            cookies: Map<String, String>,
            semester: Semester,
            checkForChanges: Boolean,
            gradesMap: MutableGradesMap,
            syncResult: SyncResult?
    ) {
        val gradesHtml = GradesFetcher.getGradesElements(cookies, semester)
        val grades = GradesParser.parseGrades(gradesHtml, syncResult)

        gradesMap.takeIf { checkForChanges }
                ?.getOrElse(semester.value) { emptyList() }
                ?.let {
                    checkForDiffs(context, accountId, it, grades, syncResult)
                }
        gradesMap[semester.value] = grades
    }

    @WorkerThread
    private fun checkForDiffs(
            context: Context,
            accountId: String,
            oldGrades: List<Grade>,
            newGrades: List<Grade>,
            syncResult: SyncResult?
    ) {
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

        syncResult?.apply { stats.numInserts += inserted.size }
        syncResult?.apply { stats.numUpdates += updated.size }
        syncResult?.apply { stats.numDeletes += deleted.size }

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
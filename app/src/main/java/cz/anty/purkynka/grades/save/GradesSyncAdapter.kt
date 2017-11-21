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
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import cz.anty.purkynka.Constants
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.load.GradesFetcher
import cz.anty.purkynka.grades.load.GradesParser
import eu.codetopic.java.utils.log.Log

/**
 * @author anty
 */
class GradesSyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, false, false) {

    companion object {

        private const val LOG_TAG = "GradesSyncAdapter"

        const val CONTENT_AUTHORITY = GradesProvider.AUTHORITY
        const val SYNC_FREQUENCY = Constants.SYNC_FREQUENCY_GRADES

        const val EXTRA_SEMESTER = "cz.anty.purkynka.grades.save.$LOG_TAG.EXTRA_SEMESTER"
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

        // TODO: Use account id, check is logged in (locally and on server)

        data.lastSyncResult = -1

        try {
            val cookies = GradesFetcher.login(loginData.username, loginData.password)
            val gradesHtml = GradesFetcher.getGradesElements(cookies, semester)

            val grades = GradesParser.parseGrades(gradesHtml)

            val gradesMap = data.grades
            gradesMap[semester.value]?.apply {
                // TODO: check for differences and show notification
                clear()
                addAll(grades)
            }
            data.grades = gradesMap

            data.lastSyncResult = 1
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to refresh grades", e)

            data.lastSyncResult = 1
        }
    }
}
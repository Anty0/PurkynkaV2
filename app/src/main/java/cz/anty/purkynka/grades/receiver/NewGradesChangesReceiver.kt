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

package cz.anty.purkynka.grades.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel
import cz.anty.purkynka.grades.sync.GradesSyncAdapter
import cz.anty.purkynka.grades.sync.GradesSyncAdapter.Companion.EXTRA_ACCOUNT_ID
import cz.anty.purkynka.grades.sync.GradesSyncAdapter.Companion.EXTRA_GRADES_CHANGES
import eu.codetopic.utils.getKSerializableExtra
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.bundle.BundleSerializer
import eu.codetopic.utils.notifications.manager.create.MultiNotificationBuilder
import eu.codetopic.utils.notifications.manager.create.MultiNotificationBuilder.Companion.requestShowAll
import kotlinx.serialization.list

/**
 * @author anty
 */
class NewGradesChangesReceiver : BroadcastReceiver() {

    companion object {

        private const val LOG_TAG = "NewGradesChangesReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GradesSyncAdapter.ACTION_NEW_GRADES_CHANGES) return

        val accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
                ?: return Log.e(LOG_TAG, "onReceive",
                        IllegalArgumentException("AccountId was not received"))
        val gradesChanges = intent
                .getKSerializableExtra(EXTRA_GRADES_CHANGES, BundleSerializer.list)
                ?: return Log.e(LOG_TAG, "onReceive",
                        IllegalArgumentException("Grades changes was not received"))

        MultiNotificationBuilder.create(
                groupId = AccountNotifyGroup.idFor(accountId),
                channelId = GradesChangesNotifyChannel.ID,
                init = {
                    persistent = true
                    refreshable = true
                    data = gradesChanges
                }
        ).requestShowAll(context)
    }
}
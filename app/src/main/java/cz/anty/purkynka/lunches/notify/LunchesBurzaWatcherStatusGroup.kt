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

package cz.anty.purkynka.lunches.notify

import android.app.NotificationChannelGroup
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.R
import eu.codetopic.utils.notifications.manager.util.NotifyGroup

/**
 * @author anty
 */
class LunchesBurzaWatcherStatusGroup : NotifyGroup(ID, LunchesBurzaWatcherStatusChannel.ID) {

    companion object {

        private const val LOG_TAG = "LunchesBurzaWatcherStatusGroup"
        const val ID = "${BuildConfig.APPLICATION_ID}.lunches.notify.burza_watcher_status_group"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createGroup(context: Context): NotificationChannelGroup =
            NotificationChannelGroup(
                    id,
                    context.getText(
                            R.string.notify_group_name_lunches_burza_watcher_status
                    )
            )
}
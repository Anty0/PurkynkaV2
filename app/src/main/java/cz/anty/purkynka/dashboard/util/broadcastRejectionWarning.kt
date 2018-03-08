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

package cz.anty.purkynka.dashboard.util

import android.content.Context
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.dashboard.DashboardItem
import cz.anty.purkynka.dashboard.DashboardManager
import cz.anty.purkynka.utils.DASHBOARD_PRIORITY_BROADCAST_REJECTION_WARNING
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.receiver
import eu.codetopic.utils.ui.container.adapter.MultiAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * @author anty
 */
class BroadcastRejectionWarningDashboardManager(context: Context, accountHolder: ActiveAccountHolder,
                              adapter: MultiAdapter<DashboardItem>) :
        DashboardManager(context, accountHolder, adapter) {

    companion object {

        private const val LOG_TAG = "BroadcastRejectionWarningDashboardManager"
        private const val ID = "cz.anty.purkynka.dashboard.util.broadcastRejectionWarning"
    }

    private val updateReceiver = receiver { _, _ -> update() }

    override fun register(): Job? {
        LocalBroadcast.registerReceiver(
                receiver = updateReceiver,
                filter = intentFilter(
                        NotifyManager.getOnChangeBroadcastAction()
                )
        )

        return update()
    }

    override fun unregister(): Job? {
        LocalBroadcast.unregisterReceiver(updateReceiver)

        return null
    }

    override fun update(): Job? {
        val adapterRef = adapter.asReference()

        return launch(UI) {
            adapterRef().mapReplaceAll(
                    id = ID,
                    items = bg calcItems@ {
                        val showItem = NotifyManager.isMultiProcessBroadcastBlockerDetected()
                        if (!showItem) return@calcItems null

                        return@calcItems listOf(
                                BroadcastRejectionWarningDashboardItem()
                        )
                    }.await() ?: emptyList()
            )
        }
    }
}

class BroadcastRejectionWarningDashboardItem : DashboardItem() {

    companion object {

        private const val LOG_TAG = "BroadcastRejectionWarningDashboardItem"
    }

    override val priority: Int
        get() = DASHBOARD_PRIORITY_BROADCAST_REJECTION_WARNING

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {}

    override fun getLayoutResId(context: Context): Int =
            R.layout.item_dashboard_broadcast_rejection_warning

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
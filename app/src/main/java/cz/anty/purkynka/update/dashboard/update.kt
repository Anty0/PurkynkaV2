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

package cz.anty.purkynka.update.dashboard

import android.content.Context
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.dashboard.DashboardItem
import cz.anty.purkynka.dashboard.DashboardManager
import cz.anty.purkynka.update.notify.UpdateNotifyChannel
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.utils.DASHBOARD_PRIORITY_UPDATE_AVAILABLE
import eu.codetopic.utils.broadcast
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.ui.container.adapter.MultiAdapter
import kotlinx.android.synthetic.main.item_update_available.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.sdk25.coroutines.onClick

/**
 * @author anty
 */
class UpdateCheckDashboardManager(context: Context, accountHolder: ActiveAccountHolder,
                                  adapter: MultiAdapter<DashboardItem>) :
        DashboardManager(context, accountHolder, adapter) {

    companion object {

        private const val LOG_TAG = "NewGradesDashboardManager"
        private const val ID = "cz.anty.purkynka.grades.dashboard.$LOG_TAG"
    }

    private val updateReceiver = broadcast { _, _ -> update() }

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
                        NotifyManager.getAllData(
                                groupId = UpdateNotifyGroup.ID,
                                channelId = UpdateNotifyChannel.ID
                        ).mapNotNull {
                            it.value.let {
                                UpdateAvailableDashboardItem(
                                        versionCode = UpdateNotifyChannel.readDataVersionCode(it)
                                                ?: return@let null,
                                        versionName = UpdateNotifyChannel.readDataVersionName(it)
                                                ?: return@let null
                                )
                            }
                        }
                    }.await()
            )
        }
    }
}

class UpdateAvailableDashboardItem(val versionCode: Int, val versionName: String) : DashboardItem() {

    override val priority: Int
        get() = DASHBOARD_PRIORITY_UPDATE_AVAILABLE

    override fun onBindViewHolder(holder: ViewHolder, itemPosition: Int) {
        holder.txtUpdateInfo.text = holder.context.getFormattedText(
                R.string.item_update_available_subtitle,
                BuildConfig.VERSION_NAME, versionName
        )

        if (itemPosition != NO_POSITION) {
            holder.boxClickTarget.setOnClickListener {

            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getItemLayoutResId(context: Context): Int = R.layout.item_update_available
}
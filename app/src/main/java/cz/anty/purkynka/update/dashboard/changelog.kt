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
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.dashboard.DashboardItem
import cz.anty.purkynka.dashboard.DashboardManager
import cz.anty.purkynka.dashboard.SwipeableDashboardItem
import cz.anty.purkynka.update.CHANGELOG_MAP
import cz.anty.purkynka.update.VersionInfo
import cz.anty.purkynka.update.inflateChangesLayout
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.update.notify.VersionChangesNotifyChannel
import cz.anty.purkynka.update.ui.VersionChangesActivity
import cz.anty.purkynka.utils.DASHBOARD_PRIORITY_VERSION_CHANGES
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.notifications.manager.data.cancel
import eu.codetopic.utils.receiver
import eu.codetopic.utils.ui.container.adapter.MultiAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.android.synthetic.main.item_dashboard_version_changes.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * @author anty
 */
class VersionChangesDashboardManager(context: Context, accountHolder: ActiveAccountHolder,
                                     adapter: MultiAdapter<DashboardItem>) :
        DashboardManager(context, accountHolder, adapter) {

    companion object {

        private const val LOG_TAG = "UpdateCheckDashboardManager"
        private const val ID = "cz.anty.purkynka.update.dashboard.changelog"
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
                        NotifyManager.getAllData(
                                groupId = UpdateNotifyGroup.ID,
                                channelId = VersionChangesNotifyChannel.ID
                        ).mapNotNull map@ {
                            val (_, data) = it
                            val versionCode = VersionChangesNotifyChannel
                                    .readDataVersionCode(data) ?: return@map null
                            val versionInfo = CHANGELOG_MAP[versionCode] ?: return@map null
                            return@map VersionChangesDashboardItem(versionCode, versionInfo)
                        }
                    }.await()
            )
        }
    }
}

class VersionChangesDashboardItem(val versionCode: Int,
                                  val versionInfo: VersionInfo) : SwipeableDashboardItem() {

    override val priority: Int
        get() = DASHBOARD_PRIORITY_VERSION_CHANGES

    override fun getSwipeDirections(holder: CustomItemViewHolder): Int = LEFT or RIGHT

    override fun onSwiped(holder: CustomItemViewHolder, direction: Int) {
        val contextRef = holder.context.asReference()
        launch(UI) notifyCancel@ {
            val notifyId = bg {
                NotifyManager.getAllData(
                        groupId = UpdateNotifyGroup.ID,
                        channelId = VersionChangesNotifyChannel.ID
                ).entries.firstOrNull {
                    versionCode == VersionChangesNotifyChannel.readDataVersionCode(it.value)
                }?.key
            }.await() ?: return@notifyCancel

            notifyId.cancel(contextRef())
        }
    }

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {
        holder.txtVersion.text = holder.context.getFormattedText(
                R.string.item_version_name,
                versionInfo.name
        )

        holder.boxChanges.apply {
            removeAllViews()
            versionInfo.inflateChangesLayout(this, 5)
        }

        if (itemPosition != NO_POSITION) {
            holder.boxClickTarget.setOnClickListener {
                VersionChangesActivity.start(holder.context, versionCode)
            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getLayoutResId(context: Context): Int = R.layout.item_dashboard_version_changes

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VersionChangesDashboardItem

        if (versionCode != other.versionCode) return false
        if (versionInfo != other.versionInfo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = versionCode
        result = 31 * result + versionInfo.hashCode()
        return result
    }
}
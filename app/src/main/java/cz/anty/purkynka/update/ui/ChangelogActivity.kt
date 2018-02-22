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

package cz.anty.purkynka.update.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import cz.anty.purkynka.R
import cz.anty.purkynka.update.CHANGELOG_MAP
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.update.notify.VersionChangesNotifyChannel
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.edit
import eu.codetopic.utils.getIconics
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.receiver
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.container.adapter.CustomItemAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.recycler.Recycler
import eu.codetopic.utils.ui.view.holder.loading.LoadingModularActivity
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * @author anty
 */
class ChangelogActivity : LoadingModularActivity(ToolbarModule(), BackButtonModule()) {

    companion object {

        private const val LOG_TAG = "ChangelogActivity"

        fun getIntent(context: Context): Intent =
                Intent(context, ChangelogActivity::class.java)

        fun start(context: Context) =
                context.startActivity(getIntent(context))
    }

    private val updateReceiver = receiver { _, _ -> update() }

    private var highlightCodes: List<Int> = emptyList()

    private var recyclerManager: Recycler.RecyclerManagerImpl? = null
    private var adapter: CustomItemAdapter<CustomItem>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = CustomItemAdapter(this)

        recyclerManager = Recycler.inflate().withItemDivider().on(this)
                .setEmptyImage(getIconics(GoogleMaterial.Icon.gmd_track_changes).sizeDp(72))
                .setEmptyText(R.string.empty_view_text_changelog)
                .setSmallEmptyText(R.string.empty_view_text_small_changelog)
                .setAdapter(adapter)

        updateWithLoading()
    }

    override fun onDestroy() {
        recyclerManager = null
        adapter = null

        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()

        register()
    }

    override fun onStop() {
        unregister()

        super.onStop()
    }

    private fun register() {
        LocalBroadcast.registerReceiver(
                receiver = updateReceiver,
                filter = intentFilter(
                        NotifyManager.getOnChangeBroadcastAction()
                )
        )
    }

    private fun unregister() {
        LocalBroadcast.unregisterReceiver(updateReceiver)
    }

    private fun updateWithLoading(): Job {
        val holder = holder
        val self = this.asReference()
        return launch(UI) {
            holder.showLoading()

            self().update().join()

            holder.hideLoading()
        }
    }

    private fun update(): Job {
        val self = this.asReference()
        return launch(UI) {
            self().highlightCodes = bg {
                NotifyManager.getAllData(
                        groupId = UpdateNotifyGroup.ID,
                        channelId = VersionChangesNotifyChannel.ID
                ).mapNotNull { VersionChangesNotifyChannel.readDataVersionCode(it.value) }
            }.await()

            self().updateUi()
        }
    }

    private fun updateUi() {
        adapter?.edit {
            clear()
            addAll(
                    CHANGELOG_MAP.entries.reversed().map {
                        val (code, info) = it
                        val highlight = code in highlightCodes
                        return@map VersionChangesItem(
                                versionCode = code,
                                versionInfo = info,
                                highlight = highlight
                        )
                    }
            )
            notifyAllItemsChanged()
        }
    }
}
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
import cz.anty.purkynka.R
import cz.anty.purkynka.update.CHANGELOG_MAP
import cz.anty.purkynka.update.getChangesText
import cz.anty.purkynka.update.inflateChangesLayout
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.update.notify.VersionChangesNotifyChannel
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.notifications.manager.data.requestCancel
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import kotlinx.android.synthetic.main.activity_version_changes.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * @author anty
 */
class VersionChangesActivity : ModularActivity(ToolbarModule(), BackButtonModule()) {

    companion object {

        private const val LOG_TAG = "VersionChangesActivity"

        private const val EXTRA_VERSION_CODE =
                "cz.anty.purkynka.update.ui.$LOG_TAG.EXTRA_VERSION_CODE"

        fun getIntent(context: Context, versionCode: Int): Intent =
                Intent(context, VersionChangesActivity::class.java)
                        .putExtra(EXTRA_VERSION_CODE, versionCode)

        fun start(context: Context, versionCode: Int) =
                context.startActivity(getIntent(context, versionCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_version_changes)

        val versionCode = intent?.getIntExtra(EXTRA_VERSION_CODE, -1)
                ?.takeIf { it != -1 }
                ?: run {
                    Log.e(LOG_TAG, "Can't create $LOG_TAG: No VersionCode received")
                    finish()
                    return
                }
        val versionInfo = CHANGELOG_MAP[versionCode]
                ?: run {
                    Log.e(LOG_TAG, "Can't create $LOG_TAG: " +
                            "Failed to locate VersionInfo for VersionCode '$versionCode'")
                    finish()
                    return
                }

        title = getFormattedText(
                R.string.title_activity_version_changes,
                versionInfo.name
        )

        versionInfo.inflateChangesLayout(boxVersionChanges)

        val self = this.asReference()
        launch(UI) notifyCancel@ {
            val notifyId = bg {
                NotifyManager.getAllData(
                        groupId = UpdateNotifyGroup.ID,
                        channelId = VersionChangesNotifyChannel.ID
                ).entries.firstOrNull {
                    versionCode == VersionChangesNotifyChannel.readDataVersionCode(it.value)
                }?.key
            }.await() ?: return@notifyCancel

            notifyId.requestCancel(self())
        }
    }
}
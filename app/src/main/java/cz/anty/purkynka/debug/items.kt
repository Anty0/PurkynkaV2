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

package cz.anty.purkynka.debug

import android.content.Context
import android.content.Intent
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.R
import cz.anty.purkynka.update.CHANGELOG_MAP
import cz.anty.purkynka.update.receiver.AppUpdatedReceiver
import cz.anty.purkynka.update.save.UpdateData
import cz.anty.purkynka.update.sync.Updater
import eu.codetopic.java.utils.letIfNull
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.sendSuspendOrderedBroadcast
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.android.synthetic.main.item_debug_app.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.longToast
import org.jetbrains.anko.sdk25.coroutines.onClick

/**
 * @author anty
 */
class AppDebugItem : CustomItem() {

    companion object {

        private const val LOG_TAG = "IssuesDebugItem"
    }

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {
        holder.butFakeUpdateFetch.onClick {
            val contextRef = holder.context.asReference()
            launch(UI) {
                val result = bg { Updater.fetchFakeUpdates() }.await()
                Updater.suspendNotifyAboutUpdate(contextRef)

                contextRef().longToast(
                        contextRef().getFormattedText(
                                R.string.item_debug_toast_fake_update_fetch_result,
                                result.toString()
                        )
                )
            }
        }

        holder.butFakeVersionChanges.onClick {
            val contextRef = holder.context.asReference()
            launch(UI) {
                bg {
                    val last = UpdateData.instance.lastKnownVersion
                    val current = BuildConfig.VERSION_CODE

                    if (last == current) {
                        UpdateData.instance.lastKnownVersion =
                                CHANGELOG_MAP.keys.max().letIfNull { current } - 1
                    }
                }.await()

                contextRef().sendSuspendOrderedBroadcast(
                        Intent(contextRef(), AppUpdatedReceiver::class.java)
                                .setAction(AppUpdatedReceiver.ACTION_FAKE_MY_PACKAGE_REPLACED)
                )

                contextRef().longToast(contextRef().getText(
                        R.string.item_debug_toast_fake_version_changes_result))
            }
        }
    }

    override fun getLayoutResId(context: Context) = R.layout.item_debug_app
}
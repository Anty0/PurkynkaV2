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

package cz.anty.purkynka.feedback.dashboard

import android.content.Context
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.dashboard.DashboardItem
import cz.anty.purkynka.dashboard.DashboardManager
import cz.anty.purkynka.dashboard.SwipeableDashboardItem
import cz.anty.purkynka.feedback.save.FeedbackData
import cz.anty.purkynka.utils.DASHBOARD_PRIORITY_ERROR_FEEDBACK
import cz.anty.purkynka.utils.URL_FACEBOOK_PAGE
import eu.codetopic.utils.AndroidUtils
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.receiver
import eu.codetopic.utils.ui.container.adapter.MultiAdapter
import kotlinx.android.synthetic.main.item_error_feedback.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.longToast

/**
 * @author anty
 */
class ErrorFeedbackDashboardManager(context: Context, accountHolder: ActiveAccountHolder,
                                    adapter: MultiAdapter<DashboardItem>) :
        DashboardManager(context, accountHolder, adapter) {

    companion object {

        private const val LOG_TAG = "NewGradesDashboardManager"
        private const val ID = "cz.anty.purkynka.feedback.dashboard.exception"
    }

    private val updateReceiver = receiver { _, _ -> update() }

    override fun register(): Job? {
        LocalBroadcast.registerReceiver(
                receiver = updateReceiver,
                filter = intentFilter(
                        FeedbackData.getter
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
                        if (FeedbackData.instance.isFeedbackEnabled) {
                            listOf(ErrorFeedbackDashboardItem())
                        } else emptyList()
                    }.await()
            )
        }
    }
}

class ErrorFeedbackDashboardItem : SwipeableDashboardItem() {

    companion object {

        private const val LOG_TAG = "NewGradeDashboardItem"
    }

    override val priority: Int
        get() = DASHBOARD_PRIORITY_ERROR_FEEDBACK

    override fun getSwipeDirections(holder: ViewHolder): Int = LEFT or RIGHT

    override fun onSwiped(holder: ViewHolder, direction: Int) {
        bg { FeedbackData.instance.notifyFeedbackDone() }
    }

    override fun onBindViewHolder(holder: ViewHolder, itemPosition: Int) {
        if (itemPosition != NO_POSITION) { // detects usage in header
            holder.boxClickTarget.setOnClickListener {
                val success = AndroidUtils.openUri(
                        holder.context,
                        URL_FACEBOOK_PAGE,
                        R.string.toast_browser_failed
                )

                if (success) {
                    holder.context.longToast(R.string.item_error_feedback_click_hint)
                    bg { FeedbackData.instance.notifyFeedbackDone() }
                }
            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getItemLayoutResId(context: Context): Int = R.layout.item_error_feedback

}
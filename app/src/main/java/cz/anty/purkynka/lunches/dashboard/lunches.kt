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

package cz.anty.purkynka.lunches.dashboard

import android.content.Context
import android.content.Intent
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.dashboard.DashboardItem
import cz.anty.purkynka.dashboard.DashboardManager
import cz.anty.purkynka.dashboard.SwipeableDashboardItem
import cz.anty.purkynka.lunches.LunchesOrderFragment
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import cz.anty.purkynka.lunches.data.LunchOptionsGroup.Companion.dateStrShort
import cz.anty.purkynka.lunches.notify.LunchesChangesNotifyChannel
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.lunches.ui.LunchOptionsGroupActivity
import cz.anty.purkynka.utils.DASHBOARD_PRIORITY_LUNCHES_NEW
import eu.codetopic.java.utils.Anchor
import eu.codetopic.java.utils.fillToLen
import eu.codetopic.java.utils.letIfNull
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.to
import eu.codetopic.utils.*
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.notifications.manager.data.NotifyId
import eu.codetopic.utils.notifications.manager.data.requestCancel
import eu.codetopic.utils.thread.LooperUtils
import eu.codetopic.utils.ui.activity.navigation.NavigationActivity
import eu.codetopic.utils.ui.container.adapter.MultiAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.android.synthetic.main.item_dashboard_lunches_new.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.textColorResource
import java.util.*

/**
 * @author anty
 */
class NewLunchesDashboardManager(context: Context, accountHolder: ActiveAccountHolder,
                                 adapter: MultiAdapter<DashboardItem>) :
        DashboardManager(context, accountHolder, adapter) {

    companion object {

        private const val LOG_TAG = "NewLunchesDashboardManager"
        private const val ID = "cz.anty.purkynka.lunches.dashboard.lunches"
    }

    private val updateReceiver = receiver { _, _ -> update() }

    override fun register(): Job? {
        LocalBroadcast.registerReceiver(
                receiver = updateReceiver,
                filter = intentFilter(
                        LunchesLoginData.getter,
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
        val accountId = accountHolder.accountId
                ?: run {
                    adapter.mapRemoveAll(ID)
                    return null
                }
        val adapterRef = adapter.asReference()

        return launch(UI) {
            adapterRef().mapReplaceAll(
                    id = ID,
                    items = bg calcItems@ {
                        val userLoggedIn = LunchesLoginData.loginData.isLoggedIn(accountId)
                        if (!userLoggedIn) return@calcItems null

                        return@calcItems NotifyManager.getAllData(
                                groupId = AccountNotifyGroup.idFor(accountId),
                                channelId = LunchesChangesNotifyChannel.ID
                        ).mapNotNull map@ {
                            val (id, data) = it
                            val lunch = LunchesChangesNotifyChannel.readData(data)
                                    ?: return@map null
                            return@map NewLunchDashboardItem(id, accountId, lunch)
                        }
                    }.await() ?: emptyList()
            )
        }
    }
}

class NewLunchDashboardItem(val notifyId: NotifyId, val accountId: String,
                            val lunchOptionsGroup: LunchOptionsGroup) : SwipeableDashboardItem() {

    companion object {

        private const val LOG_TAG = "NewLunchDashboardItem"
    }

    override val priority: Int
        get() = DASHBOARD_PRIORITY_LUNCHES_NEW

    override fun getSwipeDirections(holder: CustomItemViewHolder): Int = LEFT or RIGHT

    override fun onSwiped(holder: CustomItemViewHolder, direction: Int) {
        notifyId.requestCancel(holder.context)
    }

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {
        holder.txtDay.apply {
            text = Calendar.getInstance()
                    .apply { timeInMillis = lunchOptionsGroup.date }
                    .get(Calendar.DAY_OF_WEEK)
                    .let {
                        when (it) {
                            Calendar.SUNDAY -> R.string.txt_day_short_sunday
                            Calendar.MONDAY -> R.string.txt_day_short_monday
                            Calendar.TUESDAY -> R.string.txt_day_short_tuesday
                            Calendar.WEDNESDAY -> R.string.txt_day_short_wednesday
                            Calendar.THURSDAY -> R.string.txt_day_short_thursday
                            Calendar.FRIDAY -> R.string.txt_day_short_friday
                            Calendar.SATURDAY -> R.string.txt_day_short_saturday
                            else -> R.string.txt_day_short_unknown
                        }
                    }
                    .let { holder.context.getText(it) }
            textColorResource = when {
                // Lunch is ordered
                lunchOptionsGroup.orderedOption != null -> R.color.materialGreen
                // TODO: If lunch is not ordered (and can't be ordered) and is available in burza use materialRed
                // Lunch is not ordered and can't be ordered
                lunchOptionsGroup.options?.all { !it.enabled } != false -> R.color.materialBlue
                // Lunch is not ordered, but still can be ordered
                else -> R.color.materialOrange
            }
        }

        holder.txtDate.text = lunchOptionsGroup.dateStrShort.fillToLen(7, Anchor.RIGHT)

        val (orderedIndex, orderedLunch) = lunchOptionsGroup.orderedOption ?: null to null

        holder.txtName.text = orderedLunch?.name
                ?: holder.context.getText(R.string.text_view_lunches_no_lunch_ordered)

        holder.txtOrderedIndex.text = run orderedText@ {
            orderedIndex
                    ?.let { it + 1 }
                    ?.let {
                        holder.context.getFormattedText(
                                R.string.text_view_lunches_ordered_index,
                                it
                        )
                    }
                    ?:
                    run {
                        lunchOptionsGroup.options
                                ?.filter { it.enabled }
                                ?.count()
                                .letIfNull { 0 }
                                .let {
                                    holder.context.getFormattedQuantityText(
                                            R.plurals.text_view_lunches_available_count,
                                            it, it
                                    )
                                }
                    }
        }

        if (itemPosition != NO_POSITION) { // detects usage in header
            holder.boxClickTarget.setOnClickListener {
                notifyId.requestCancel(holder.context)

                /*val contextRef = holder.context.asReference()
                launch(UI) fragment@ {
                    delay(500)
                    contextRef().baseActivity
                            ?.to<NavigationActivity>()
                            ?.replaceFragment(LunchesOrderFragment::class.java)
                }*/

                run activity@ {
                    val context = holder.context
                    val options = context.baseActivity?.let {
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                it,
                                holder.boxTransition,
                                context.getString(R.string.id_transition_lunch_options_group_item)
                        )
                    }

                    if (options == null) Log.e(LOG_TAG, "Can't start LunchOptionsGroupActivity " +
                            "with transition: Cannot find Activity in context hierarchy")

                    ContextCompat.startActivity(
                            context,
                            LunchOptionsGroupActivity.getStartIntent(
                                    context, accountId, lunchOptionsGroup),
                            options?.toBundle()
                    )
                }
            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getLayoutResId(context: Context): Int = R.layout.item_dashboard_lunches_new

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewLunchDashboardItem

        if (notifyId != other.notifyId) return false
        if (accountId != other.accountId) return false
        if (lunchOptionsGroup != other.lunchOptionsGroup) return false

        return true
    }

    override fun hashCode(): Int {
        var result = notifyId.hashCode()
        result = 31 * result + accountId.hashCode()
        result = 31 * result + lunchOptionsGroup.hashCode()
        return result
    }
}
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
import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.dashboard.DashboardItem
import cz.anty.purkynka.dashboard.DashboardManager
import cz.anty.purkynka.lunches.LunchesOrderFragment
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.utils.DASHBOARD_PRIORITY_LUNCHES_CREDIT
import eu.codetopic.java.utils.to
import eu.codetopic.utils.baseActivity
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.receiver
import eu.codetopic.utils.ui.activity.navigation.NavigationActivity
import eu.codetopic.utils.ui.container.adapter.MultiAdapter
import kotlinx.android.synthetic.main.item_dashboard_lunches_credit.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import java.text.DecimalFormat

/**
 * @author anty
 */
class LunchesCreditDashboardManager(context: Context, accountHolder: ActiveAccountHolder,
                                    adapter: MultiAdapter<DashboardItem>) :
        DashboardManager(context, accountHolder, adapter) {

    companion object {

        private const val LOG_TAG = "LunchesCreditDashboardManager"
        private const val ID = "cz.anty.purkynka.lunches.dashboard.credit"
    }

    private val updateReceiver = receiver { _, _ -> update() }

    override fun register(): Job? {
        LocalBroadcast.registerReceiver(
                receiver = updateReceiver,
                filter = intentFilter(
                        LunchesLoginData.getter,
                        LunchesData.getter
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
                        // TODO: create LunchesPreferences and add there option to disable this item
                        // TODO: add swipe support (to disable item) and show snackbar with option to revert that

                        val userLoggedIn = LunchesLoginData.loginData.isLoggedIn(accountId)
                        if (!userLoggedIn) return@calcItems null

                        return@calcItems LunchesData.instance.getCredit(accountId)
                                .takeIf { it < 90 }
                                ?.let { LunchesCreditDashboardItem(it) }
                                ?.let { listOf(it) }
                    }.await() ?: emptyList()
            )
        }
    }
}

class LunchesCreditDashboardItem(val credit: Float) : DashboardItem() {

    companion object {

        private const val LOG_TAG = "LunchesCreditDashboardItem"

        private val FORMAT_CREDIT = DecimalFormat("#.##")
    }

    override val priority: Int
        get() = DASHBOARD_PRIORITY_LUNCHES_CREDIT

    override fun onBindViewHolder(holder: ViewHolder, itemPosition: Int) {
        holder.txtCredit.text = SpannableStringBuilder().apply {
            append(holder.context.getText(R.string.text_view_credit))
            append(
                    SpannableStringBuilder().apply {
                        val colorSpan = ForegroundColorSpan(
                                ContextCompat.getColor(
                                        holder.context,
                                        when {
                                            credit.isNaN() -> R.color.materialBlue
                                            credit < 60 -> R.color.materialRed
                                            credit < 90 -> R.color.materialYellow
                                            else -> R.color.materialGreen
                                        }
                                )
                        )

                        setSpan(colorSpan, 0, 0, Spanned.SPAN_MARK_MARK)
                        append(
                                if (credit.isNaN()) "?"
                                else FORMAT_CREDIT.format(credit)
                        )
                        append(",- Kč")
                        setSpan(colorSpan, 0, this.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
            )
        }

        if (itemPosition != NO_POSITION) { // detects usage in header
            holder.boxClickTarget.setOnClickListener {
                holder.context.baseActivity
                        ?.to<NavigationActivity>()
                        ?.replaceFragment(LunchesOrderFragment::class.java)
            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getItemLayoutResId(context: Context): Int = R.layout.item_dashboard_lunches_credit

}
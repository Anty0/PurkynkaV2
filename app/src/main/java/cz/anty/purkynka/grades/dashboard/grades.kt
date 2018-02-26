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

package cz.anty.purkynka.grades.dashboard

import android.content.Context
import android.graphics.Typeface
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import android.view.View
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.dashboard.DashboardItem
import cz.anty.purkynka.dashboard.DashboardManager
import cz.anty.purkynka.grades.data.Grade
import cz.anty.purkynka.grades.data.Grade.Companion.valueColor
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesPreferences
import cz.anty.purkynka.grades.ui.GradeActivity
import cz.anty.purkynka.utils.DASHBOARD_PRIORITY_GRADES_NEW
import eu.codetopic.java.utils.Anchor
import eu.codetopic.java.utils.fillToLen
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.*
import eu.codetopic.utils.baseActivity
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.ui.container.adapter.MultiAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItemViewHolder
import kotlinx.android.synthetic.main.item_dashboard_grade_new.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * @author anty
 */
class NewGradesDashboardManager(context: Context, accountHolder: ActiveAccountHolder,
                                adapter: MultiAdapter<DashboardItem>) :
        DashboardManager(context, accountHolder, adapter) {

    companion object {

        private const val LOG_TAG = "NewGradesDashboardManager"
        private const val ID = "cz.anty.purkynka.grades.dashboard.grades"
    }

    private val updateReceiver = receiver { _, _ -> update() }

    override fun register(): Job? {
        LocalBroadcast.registerReceiver(
                receiver = updateReceiver,
                filter = intentFilter(
                        GradesLoginData.getter,
                        GradesPreferences.getter,
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
                        val userLoggedIn = GradesLoginData.loginData.isLoggedIn(accountId)
                        if (!userLoggedIn) return@calcItems null

                        val badAverage = GradesPreferences.instance.badAverage

                        return@calcItems NotifyManager.getAllData(
                                groupId = AccountNotifyGroup.idFor(accountId),
                                channelId = GradesChangesNotifyChannel.ID
                        ).mapNotNull {
                            it.value.let {
                                val grade = GradesChangesNotifyChannel.readDataGrade(it)
                                        ?: return@let null
                                val gradeChanges = GradesChangesNotifyChannel.readDataChanges(it)

                                return@let NewGradeDashboardItem(
                                        grade = grade,
                                        isBad = badAverage <= grade.value,
                                        changes = gradeChanges
                                )
                            }
                        }
                    }.await() ?: emptyList()
            )
        }
    }
}

class NewGradeDashboardItem(val grade: Grade, val isBad: Boolean, val changes: List<String>? = null) : DashboardItem() {

    companion object {

        private const val LOG_TAG = "NewGradeDashboardItem"
    }

    private val valueColor = grade.valueColor

    override val priority: Int
        get() = DASHBOARD_PRIORITY_GRADES_NEW

    override fun onBindViewHolder(holder: CustomItemViewHolder, itemPosition: Int) {
        holder.txtSubject.apply {
            setTextColor(valueColor)
            text = grade.subjectShort.fillToLen(4, Anchor.LEFT)
        }

        holder.txtGrade.apply {
            setTypeface(null, if (isBad) Typeface.BOLD else Typeface.NORMAL)
            //setTextColor(valueColor)
            text = grade.valueToShow
        }

        holder.txtWeight.apply {
            setTypeface(null, if (grade.weight >= 3) Typeface.BOLD else Typeface.NORMAL)
            text = grade.weight.toString()
        }

        holder.txtNote.text = grade.note

        holder.txtTeacher.text = grade.teacher

        if (itemPosition != NO_POSITION) { // detects usage in header
            holder.boxClickTarget.setOnClickListener {
                val context = holder.context
                val options = context.baseActivity?.let {
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            it,
                            holder.boxTransition,
                            context.getString(R.string.id_transition_grade_item)
                    )
                }

                if (options == null) Log.e(LOG_TAG, "Can't start GradeActivity " +
                        "with transition: Cannot find Activity in context hierarchy")

                ContextCompat.startActivity(
                        context,
                        GradeActivity.getStartIntent(context, grade, isBad, true, changes),
                        options?.toBundle()
                )
            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getLayoutResId(context: Context): Int = R.layout.item_dashboard_grade_new

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewGradeDashboardItem

        if (grade != other.grade) return false
        if (changes != other.changes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grade.hashCode()
        result = 31 * result + (changes?.hashCode() ?: 0)
        return result
    }
}
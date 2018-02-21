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
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.dashboard.DashboardItem
import cz.anty.purkynka.dashboard.DashboardManager
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.data.Subject
import cz.anty.purkynka.grades.data.Subject.Companion.average
import cz.anty.purkynka.grades.data.Subject.Companion.averageColor
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesPreferences
import cz.anty.purkynka.grades.ui.SubjectActivity
import cz.anty.purkynka.utils.DASHBOARD_PRIORITY_GRADES_SUBJECTS_AVERAGE_BAD
import eu.codetopic.java.utils.Anchor
import eu.codetopic.java.utils.fillToLen
import eu.codetopic.java.utils.format
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.*
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.ui.container.adapter.MultiAdapter
import kotlinx.android.synthetic.main.item_subject_bad_average.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * @author anty
 */
class SubjectsAverageDashboardManager(context: Context, accountHolder: ActiveAccountHolder,
                                      adapter: MultiAdapter<DashboardItem>) :
        DashboardManager(context, accountHolder, adapter) {

    companion object {

        private const val LOG_TAG = "SubjectsAverageDashboardManager"
        private const val ID = "cz.anty.purkynka.grades.dashboard.$LOG_TAG"
    }

    private val updateReceiver = receiver { _, _ -> update() }

    override fun register(): Job? {
        LocalBroadcast.registerReceiver(
                receiver = updateReceiver,
                filter = intentFilter(
                        GradesLoginData.getter,
                        GradesData.getter,
                        GradesPreferences.getter
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

                        val badAverage = GradesPreferences.instance.subjectBadAverage
                        val semester = Semester.AUTO.stableSemester
                        return@calcItems GradesData.instance
                                .getSubjects(accountId)[semester.value]
                                ?.filter { subject -> subject.average > badAverage }
                                ?.map { BadSubjectAverageDashboardItem(accountId, it) }
                    }.await() ?: emptyList()
            )
        }
    }
}

class BadSubjectAverageDashboardItem(val accountId: String,
                                     val subject: Subject) : DashboardItem() {

    companion object {

        private const val LOG_TAG = "BadSubjectAverageDashboardItem"
    }

    private val average: Double = subject.average
    private val averageColor: Int = subject.averageColor

    override val priority: Int
        get() = DASHBOARD_PRIORITY_GRADES_SUBJECTS_AVERAGE_BAD + (average * 100).toInt()

    override fun onBindViewHolder(holder: ViewHolder, itemPosition: Int) {
        holder.txtNameShort.text = subject.shortName.fillToLen(4, Anchor.LEFT)

        holder.txtAverage.apply {
            setTextColor(averageColor)
            text = average.format(2)
        }

        holder.txtNameLong.text = subject.fullName

        val gradesCount = subject.grades.size
        holder.txtInfoAndGradesCount.text =
                holder.context.getFormattedText(
                        R.string.item_subject_bad_average_info_subtitle,
                        holder.context.getFormattedQuantityText(
                                R.plurals.text_view_grades_count,
                                gradesCount, gradesCount
                        )
                )

        if (itemPosition != NO_POSITION) { // detects usage in header
            holder.boxClickTarget.setOnClickListener {
                val context = holder.context
                val options = context.baseActivity?.let {
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                            it,
                            holder.boxFrame,
                            context.getString(R.string.id_transition_subject_item)
                    )
                }

                if (options == null) Log.e(LOG_TAG, "Can't start SubjectActivity " +
                        "with transition: Cannot find Activity in context hierarchy")

                ContextCompat.startActivity(
                        context,
                        SubjectActivity.getStartIntent(context, subject),
                        options?.toBundle()
                )
            }
        } else holder.boxClickTarget.setOnClickListener(null)
    }

    override fun getItemLayoutResId(context: Context): Int = R.layout.item_subject_bad_average
}
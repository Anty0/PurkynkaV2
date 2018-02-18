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
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.dashboard.DashboardItem
import cz.anty.purkynka.dashboard.DashboardManager
import cz.anty.purkynka.dashboard.SwipeableDashboardItem
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.data.Subject
import cz.anty.purkynka.grades.data.Subject.Companion.average
import cz.anty.purkynka.grades.data.Subject.Companion.averageColor
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesPreferences
import cz.anty.purkynka.grades.ui.SubjectActivity
import cz.anty.purkynka.utils.Constants.DASHBOARD_PRIORITY_GRADES_SUBJECTS_AVERAGE_BAD
import eu.codetopic.java.utils.JavaExtensions
import eu.codetopic.java.utils.JavaExtensions.fillToLen
import eu.codetopic.java.utils.JavaExtensions.format
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.AndroidExtensions.getFormattedText
import eu.codetopic.utils.AndroidExtensions.getFormattedQuantityText
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.ui.container.adapter.MultiAdapter
import kotlinx.android.synthetic.main.item_subject_bad_average.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.longToast

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

    private val updateReceiver = broadcast { _, _ -> update() }

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
            val badSubjectsItems = bg calcItems@ {
                val userLoggedIn = GradesLoginData.loginData.isLoggedIn(accountId)
                if (!userLoggedIn) return@calcItems null

                //val dismissedSubjects = GradesPreferences.instance.getDismissedSubjects(accountId)
                val badAverage = GradesPreferences.instance.subjectBadAverage
                val semester = Semester.AUTO.stableSemester
                return@calcItems GradesData.instance
                        .getSubjects(accountId)[semester.value]
                        ?.filter { subject ->
                            subject.average > badAverage
                                    //&& dismissedSubjects.none { it.idEquals(semester, subject) }
                        }
                        ?.map { BadSubjectAverageDashboardItem(accountId, semester, it) }
            }.await()

            adapterRef().mapReplaceAll(ID, badSubjectsItems ?: emptyList())
        }
    }
}

class BadSubjectAverageDashboardItem(val accountId: String, val semester: Semester,
                                     val subject: Subject) : /*Swipeable*/DashboardItem() {

    private val average: Double = subject.average
    private val averageColor: Int = subject.averageColor

    override val priority: Int
        get() = DASHBOARD_PRIORITY_GRADES_SUBJECTS_AVERAGE_BAD + (average * 100).toInt()

    /*override fun getSwipeDirections(holder: ViewHolder): Int = LEFT or RIGHT

    override fun onSwiped(holder: ViewHolder, direction: Int) {
        bg { GradesPreferences.instance.dismissSubject(accountId, semester, subject) }
    }*/

    override fun onBindViewHolder(holder: ViewHolder, itemPosition: Int) {
        holder.txtNameShort.text = subject.shortName.fillToLen(4, JavaExtensions.Anchor.LEFT)

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
                SubjectActivity.start(holder.context, subject)
            }
        }
    }

    override fun getItemLayoutResId(context: Context): Int = R.layout.item_subject_bad_average
}
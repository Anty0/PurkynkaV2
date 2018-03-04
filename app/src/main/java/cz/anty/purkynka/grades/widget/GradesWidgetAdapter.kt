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

package cz.anty.purkynka.grades.widget

import android.content.Context
import android.widget.RemoteViews
import cz.anty.purkynka.R
import cz.anty.purkynka.account.notify.AccountNotifyGroup
import cz.anty.purkynka.grades.data.Semester
import cz.anty.purkynka.grades.data.Subject.Companion.average
import cz.anty.purkynka.grades.load.GradesParser.toSubjects
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel.Companion.readDataChanges
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel.Companion.readDataGrade
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.ui.GradeItem
import cz.anty.purkynka.grades.ui.SubjectItem
import cz.anty.purkynka.grades.util.GradesSort
import cz.anty.purkynka.grades.util.GradesSort.*
import eu.codetopic.utils.edit
import eu.codetopic.utils.notifications.manager.NotifyManager
import eu.codetopic.utils.ui.container.adapter.RemoteCustomItemAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.items.custom.LoadingItem

/**
 * @author anty
 */
class GradesWidgetAdapter(
        context: Context,
        private val accountId: String,
        private val sort: GradesSort,
        private val badAverage: Float
) : RemoteCustomItemAdapter<CustomItem>(context) {

    companion object {

        fun generateLoadingView(context: Context): RemoteViews {
            val loadingItem = LoadingItem(
                    context = context,
                    titleId = R.string.item_loading_title,
                    subtitleId = R.string.item_loading_subtitle
            )
            val holder = loadingItem.createRemoteViewHolder(context)
            loadingItem.bindRemoteViewHolder(holder, CustomItem.NO_POSITION)
            return holder.itemView
        }
    }

    override fun onDataSetChanged() {
        super.onDataSetChanged()

        updateItems()
    }

    private fun updateItems() {
        val gradesMap = GradesData.instance.getGrades(accountId)
        val gradesChanges = NotifyManager.getAllData(
                groupId = AccountNotifyGroup.idFor(accountId),
                channelId = GradesChangesNotifyChannel.ID
        ).values.mapNotNull {
            readDataGrade(it)?.id?.let { id ->
                readDataChanges(it)?.let { changes ->
                    id to changes
                }
            }
        }.toMap()

        edit {
            clear()

            val gradesList = gradesMap[Semester.AUTO.value] ?: return@edit

            val getGrades = {
                gradesList.map {
                    GradeItem(
                            base = it,
                            isBad = badAverage <= it.value,
                            changes = gradesChanges[it.id]
                    )
                }
            }
            val getSubjects = {
                gradesList.toSubjects().map {
                    SubjectItem(
                            base = it,
                            isBad = badAverage <= it.average,
                            changes = it.grades.mapNotNull { grade ->
                                gradesChanges[grade.id]?.let { grade.id to it }
                            }.toMap()
                    )
                }
            }

            @Suppress("REDUNDANT_ELSE_IN_WHEN")
            addAll(when (sort) {
                GRADES_DATE -> run(getGrades)
                GRADES_VALUE -> run(getGrades).sortedBy { it.base.value }
                GRADES_SUBJECT -> run(getGrades).sortedBy { it.base.subjectShort }
                SUBJECTS_NAME -> run(getSubjects)
                SUBJECTS_AVERAGE_BEST -> run(getSubjects).sortedBy { it.base.average }
                SUBJECTS_AVERAGE_WORSE ->
                    run(getSubjects).sortedByDescending { it.base.average }
                else -> run(getGrades)
            })
        }
    }
}
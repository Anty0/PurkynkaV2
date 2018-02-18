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

package cz.anty.purkynka.grades.ui

import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.graphics.drawable.AnimatorInflaterCompat
import android.support.transition.TransitionInflater
import android.support.transition.TransitionManager
import android.transition.Transition
import android.view.View
import android.view.animation.AnimationUtils
import cz.anty.purkynka.R
import cz.anty.purkynka.grades.data.Subject
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.getKSerializableExtra
import eu.codetopic.utils.AndroidExtensions.putKSerializableExtra
import eu.codetopic.utils.simple.SimpleTransitionListener
import eu.codetopic.utils.thread.LooperUtils
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.activity.modular.module.TransitionBackButtonModule
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.recycler.Recycler
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.list
import kotlinx.serialization.map
import org.jetbrains.anko.ctx
import org.jetbrains.anko.toast
import java.lang.ref.WeakReference

/**
 * @author anty
 */
class SubjectActivity : ModularActivity(ToolbarModule(), TransitionBackButtonModule()) {

    companion object {

        private const val LOG_TAG = "SubjectActivity"

        private const val EXTRA_SUBJECT =
                "cz.anty.purkynka.grades.ui.$LOG_TAG.EXTRA_SUBJECT"
        private const val EXTRA_SUBJECT_CHANGES =
                "cz.anty.purkynka.grades.ui.$LOG_TAG.EXTRA_SUBJECT_CHANGES"
        private val EXTRA_SUBJECT_CHANGES_SERIALIZER =
                (IntSerializer to StringSerializer.list).map

        fun getStartIntent(context: Context, subject: Subject,
                           changes: Map<Int, List<String>> = emptyMap()) =
                Intent(context, SubjectActivity::class.java)
                        .putKSerializableExtra(EXTRA_SUBJECT, subject)
                        .putKSerializableExtra(EXTRA_SUBJECT_CHANGES, changes,
                                EXTRA_SUBJECT_CHANGES_SERIALIZER)

        fun start(context: Context, subject: Subject, changes: Map<Int, List<String>> = emptyMap()) =
                context.startActivity(getStartIntent(context, subject, changes))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject)

        val subject = intent
                ?.getKSerializableExtra<Subject>(EXTRA_SUBJECT)
                ?: run {
                    Log.e(LOG_TAG, "Can't create $LOG_TAG: No Subject received")
                    finish()
                    return
                }
        val subjectChanges = intent
                ?.getKSerializableExtra(EXTRA_SUBJECT_CHANGES,
                        EXTRA_SUBJECT_CHANGES_SERIALIZER)
                ?: emptyMap()

        title = subject.fullName

        val subjectItem = SubjectItem(subject, subjectChanges)
        val itemVH = subjectItem.createViewHolder(this, boxSubject)
                .also { boxSubject.addView(it.itemView) }
        subjectItem.bindViewHolder(itemVH, CustomItem.NO_POSITION)

        Recycler.inflate().on(layoutInflater, boxRecycler, true)
                .setAdapter(
                        subject.grades.map {
                            GradeItem(
                                    base = it,
                                    showSubject = false,
                                    changes = subjectChanges[it.id]
                            )
                        }
                )
    }

    override fun onResume() {
        super.onResume()

        boxRecycler.takeIf { it.visibility == View.GONE }
                ?.apply {
                    visibility = View.VISIBLE
                    startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.slide_down))
                }
    }
}
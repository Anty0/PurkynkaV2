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
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.simple.SimpleAnimatorListener
import eu.codetopic.utils.simple.SimpleTransitionListener
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.activity.modular.module.TransitionBackButtonModule
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.recycler.Recycler
import kotlinx.android.synthetic.main.activity_subject.*
import kotlinx.serialization.json.JSON

/**
 * @author anty
 */
class SubjectActivity : ModularActivity(ToolbarModule(), TransitionBackButtonModule()) {

    companion object {

        private const val LOG_TAG = "SubjectActivity"

        private const val EXTRA_SUBJECT_ITEM =
                "cz.anty.purkynka.grades.ui.$LOG_TAG.EXTRA_SUBJECT_ITEM"

        fun getStartIntent(context: Context, subjectItem: SubjectItem) =
                Intent(context, SubjectActivity::class.java)
                        .putExtra(EXTRA_SUBJECT_ITEM, JSON.stringify(subjectItem))

        fun start(context: Context, subjectItem: SubjectItem) =
                context.startActivity(getStartIntent(context, subjectItem))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject)

        val subjectItem = intent?.getStringExtra(EXTRA_SUBJECT_ITEM)
                ?.let { JSON.parse<SubjectItem>(it) } ?: run {
            Log.e(LOG_TAG, "Can't create $LOG_TAG: No SubjectItem received")
            finish()
            return
        }

        title = subjectItem.base.fullName

        val itemVH = subjectItem.createViewHolder(this, boxSubject).also {
            boxSubject.addView(it.itemView)
        }
        subjectItem.bindViewHolder(itemVH, CustomItem.NO_POSITION)

        Recycler.inflate().on(layoutInflater, boxRecycler, true)
                .setAdapter(
                        subjectItem.base.grades.map {
                            GradeItem(
                                    base = it,
                                    showSubject = false,
                                    changes = subjectItem.changes[it.id]
                            )
                        }
                )

        if (savedInstanceState == null) {
            val recyclerAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            val showRecycler = {
                boxRecycler.apply {
                    visibility = View.VISIBLE
                    startAnimation(recyclerAnimation)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.sharedElementEnterTransition?.apply {
                    addListener(object : SimpleTransitionListener() {
                        override fun onTransitionEnd(transition: Transition) {
                            run(showRecycler)
                        }
                    })
                } ?: run(showRecycler)
            } else run(showRecycler)
        } else {
            boxRecycler.visibility = View.VISIBLE
        }
    }
}
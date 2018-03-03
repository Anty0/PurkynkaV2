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
import android.content.Intent
import android.net.Uri
import android.view.ContextThemeWrapper
import android.widget.RemoteViewsService
import cz.anty.purkynka.R
import cz.anty.purkynka.grades.util.GradesSort
import eu.codetopic.java.utils.to
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.receiver
import eu.codetopic.utils.ui.container.adapter.forWidget

/**
 * @author anty
 */
class GradesWidgetAdapterService : RemoteViewsService() {

    companion object {

        private const val LOG_TAG = "GradesWidgetAdapterService"

        private const val EXTRA_ACCOUNT_ID =
                "cz.anty.purkynka.grades.widget.$LOG_TAG.EXTRA_ACCOUNT_ID"
        private const val EXTRA_SORT_NAME =
                "cz.anty.purkynka.grades.widget.$LOG_TAG.EXTRA_SORT"
        private const val EXTRA_BAD_AVERAGE =
                "cz.anty.purkynka.grades.widget.$LOG_TAG.EXTRA_BAD_AVERAGE"

        fun getIntent(
                context: Context,
                accountId: String,
                sort: GradesSort,
                badAverage: Float
        ): Intent = Intent(context, GradesWidgetAdapterService::class.java)
                .putExtra(EXTRA_ACCOUNT_ID, accountId)
                .putExtra(EXTRA_SORT_NAME, sort.name)
                .putExtra(EXTRA_BAD_AVERAGE, badAverage)
                .also { it.data = Uri.parse(it.toUri(Intent.URI_INTENT_SCHEME)) }
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
                ?: throw IllegalArgumentException("No accountId received in intent")
        val sort = intent.getStringExtra(EXTRA_SORT_NAME)?.let { GradesSort.valueOf(it) }
                ?: throw IllegalArgumentException("No sort received in intent")
        val badAverage = intent
                .getFloatExtra(EXTRA_BAD_AVERAGE, -1F).takeIf { it != -1F }
                ?: throw IllegalArgumentException("No badAverage received in intent")

        val themedContext = ContextThemeWrapper(this, R.style.AppTheme_Grades)
        return GradesWidgetAdapter(
                context = themedContext,
                accountId = accountId,
                sort = sort,
                badAverage = badAverage
        ).forWidget(
                loadingView = GradesWidgetAdapter.generateLoadingView(themedContext)
        )
    }
}
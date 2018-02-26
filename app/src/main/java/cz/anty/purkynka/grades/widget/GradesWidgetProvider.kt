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

import android.accounts.Account
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.MainThread
import android.view.ContextThemeWrapper
import android.widget.RemoteViews
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial

import cz.anty.purkynka.R
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesPreferences
import cz.anty.purkynka.grades.sync.GradesSyncAdapter
import cz.anty.purkynka.grades.util.GradesSort
import cz.anty.purkynka.utils.ICON_GRADES
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.getIconics
import eu.codetopic.utils.ids.Identifiers
import eu.codetopic.utils.ids.Identifiers.Companion.nextId

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [GradesWidgetConfigureActivity]
 */
class GradesWidgetProvider : AppWidgetProvider() {
    companion object {

        private const val LOG_TAG = "GradesWidgetProvider"

        private const val EXTRA_REQUEST_REFRESH =
                "cz.anty.purkynka.grades.widget.$LOG_TAG.EXTRA_REQUEST_REFRESH"

        private val ID_TYPE_REFRESH =
                Identifiers.Type("cz.anty.purkynka.grades.widget.$LOG_TAG.ID_REFRESH")

        @MainThread
        private fun getUpdateIntent(context: Context, appWidgetIds: IntArray, requestRefresh: Boolean): Intent =
                Intent(context.applicationContext, GradesWidgetProvider::class.java)
                        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                        .putExtra(EXTRA_REQUEST_REFRESH, requestRefresh)

        @MainThread
        private fun getUpdateIntent(context: Context, requestRefresh: Boolean): Intent =
                getUpdateIntent(context, getAllWidgetIds(context), requestRefresh)

        @MainThread
        fun getUpdateIntent(context: Context, appWidgetIds: IntArray): Intent =
                getUpdateIntent(context, appWidgetIds, false)

        @MainThread
        fun getUpdateIntent(context: Context): Intent =
                getUpdateIntent(context, false)

        @MainThread
        fun getAllWidgetIds(context: Context): IntArray =
                AppWidgetManager.getInstance(context)
                        .getAppWidgetIds(ComponentName(context, GradesWidgetProvider::class.java))

        @MainThread
        fun notifyItemsChanged(context: Context) {
            notifyItemsChanged(context, getAllWidgetIds(context))
        }

        @MainThread
        fun notifyItemsChanged(context: Context, appWidgetIds: IntArray) {
            AppWidgetManager.getInstance(context)
                    .notifyAppWidgetViewDataChanged(appWidgetIds, R.id.boxList)
        }
    }

    private var intent: Intent? = null

    private fun doBasicSetup(context: Context, views: RemoteViews,
                             account: Account?, accountId: String?,
                             appWidgetId: Int) = views.apply {
        setTextViewText(
                R.id.txtTitle,
                account?.name?.let {
                    context.getFormattedText(
                            R.string.title_widget_grades_with_user,
                            it
                    )
                } ?: context.getText(R.string.title_widget_grades)
        )
        setImageViewBitmap(
                R.id.butRefresh,
                context.getIconics(GoogleMaterial.Icon.gmd_refresh)
                        .actionBar().toBitmap()
        )
        setOnClickPendingIntent(R.id.boxToolbar, PendingIntent.getBroadcast(
                context,
                0,
                GradesWidgetLaunchReceiver
                        .getIntent(context, accountId)
                        .also {
                            if (Build.VERSION.SDK_INT >= 16)
                                it.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        },
                PendingIntent.FLAG_UPDATE_CURRENT
        ))
        setOnClickPendingIntent(R.id.butRefresh, PendingIntent.getBroadcast(
                context,
                ID_TYPE_REFRESH.nextId(),
                getUpdateIntent(context, intArrayOf(appWidgetId), true)
                        .also {
                            if (Build.VERSION.SDK_INT >= 16)
                                it.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        },
                PendingIntent.FLAG_UPDATE_CURRENT
        ))
    }

    private fun showLoading(context: Context, appWidgetManager: AppWidgetManager,
                            appWidgetId: Int, accountId: String?, account: Account?) {
        val loadingViews = RemoteViews(
                context.packageName,
                R.layout.widget_grades
        )
        doBasicSetup(context, loadingViews,
                account, accountId, appWidgetId)

        // Setup widget content
        loadingViews.removeAllViews(R.id.boxContent)
        loadingViews.addView(
                R.id.boxContent,
                RemoteViews(
                        context.packageName,
                        R.layout.widget_grades_content_loading
                )
        )

        Log.d(LOG_TAG, "onUpdate()" +
                " -> (appWidgetId=$appWidgetId)" +
                " -> Applying loading views")
        appWidgetManager.updateAppWidget(appWidgetId, loadingViews)
    }

    private fun requestSync(accountId: String, account: Account) {
        Log.d(LOG_TAG, "onUpdate()" +
                " -> (accountId=$accountId, account=$account)" +
                " -> Requesting sync")
        GradesSyncAdapter.requestSync(account)
    }

    private fun showContent(context: Context, appWidgetManager: AppWidgetManager,
                            appWidgetId: Int, accountId: String?, account: Account?) {
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.widget_grades)
        doBasicSetup(context, views, account, accountId, appWidgetId)

        // Setup widget content
        views.removeAllViews(R.id.boxContent)
        when {
            accountId == null ->
                views.addView(
                        R.id.boxContent,
                        RemoteViews(
                                context.packageName,
                                R.layout.widget_grades_content_no_account
                        ).also content@ {
                            it.setImageViewBitmap(
                                    R.id.imgError,
                                    context.getIconics(
                                            CommunityMaterial.Icon
                                                    .cmd_alert_circle_outline
                                    ).colorRes(R.color.materialLightRed)
                                            .sizeDp(72).toBitmap()
                            )
                        }
                )
            !GradesLoginData.loginData.isLoggedIn(accountId) ->
                views.addView(
                        R.id.boxContent,
                        RemoteViews(
                                context.packageName,
                                R.layout.widget_grades_content_not_logged_in
                        ).also content@ {
                            it.setImageViewBitmap(
                                    R.id.imgError,
                                    context.getIconics(
                                            CommunityMaterial.Icon
                                                    .cmd_alert_circle_outline
                                    ).colorRes(R.color.materialLightRed)
                                            .sizeDp(72).toBitmap()
                            )
                        }
                )
            else -> {
                val gradesSort = GradesPreferences.instance
                        .getAppWidgetSort(appWidgetId)
                        ?: GradesSort.GRADES_DATE
                val badAverage = GradesPreferences.instance.badAverage


                views.addView(
                        R.id.boxContent,
                        RemoteViews(
                                context.packageName,
                                R.layout.widget_grades_content
                        ).also content@ {
                            it.setImageViewBitmap(
                                    R.id.imgEmpty,
                                    context.getIconics(ICON_GRADES)
                                            .sizeDp(72).toBitmap()
                            )

                            it.setEmptyView(R.id.boxList, R.id.boxEmptyView)
                        }
                )

                views.setRemoteAdapter(
                        R.id.boxList,
                        GradesWidgetAdapterService.getIntent(
                                context = context,
                                accountId = accountId,
                                sort = gradesSort,
                                badAverage = badAverage
                        )
                )
            }
        }

        // Instruct the widget manager to update the widget
        Log.d(LOG_TAG, "updateAppWidget()" +
                " -> (appWidgetId=$appWidgetId, accountId=$accountId, account=$account)" +
                " -> Applying views")
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Put intent in temporary location, so we can access intent in onUpdate
        this.intent = intent
        val themedContext = ContextThemeWrapper(context, R.style.AppTheme_Grades)
        super.onReceive(themedContext, intent)
        this.intent = null
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager,
                          appWidgetIds: IntArray) {
        Log.d(LOG_TAG, "onUpdate(appWidgetIds=$appWidgetIds)")

        // Extract extras from intent
        val extras = intent?.extras

        val isRefreshRequest = extras?.getBoolean(
                EXTRA_REQUEST_REFRESH,
                false
        ) ?: false

        // Extract ids AccountIds of widgets
        val availableAccounts = Accounts.getAllWIthIds(context)
        val appWidgetIdsMap = GradesPreferences.instance.let createMap@ { prefs ->
            return@createMap appWidgetIds.map {
                it to prefs.getAppWidgetAccountId(it)
                        ?.takeIf { it in availableAccounts }
            }
        }.toMap()

        if (isRefreshRequest) {
            // Show loading view in all widgets
            appWidgetIdsMap.forEach {
                val (appWidgetId, accountId) = it
                val account = accountId?.let { availableAccounts[it] }

                showLoading(context, appWidgetManager, appWidgetId, accountId, account)
            }

            // Request refresh of all accounts associated with widgets
            appWidgetIdsMap
                    .let {
                        val accountIdsMap = mutableMapOf<String?, MutableList<Int>>()
                        it.forEach {
                            val (appWidgetId, accountId) = it
                            accountIdsMap.getOrPut(accountId, ::mutableListOf).add(appWidgetId)
                            return@forEach
                        }
                        return@let accountIdsMap as Map<String?, List<Int>>
                    }
                    .forEach {
                        val (accountId, accountAppWidgetIds) = it
                        val account = accountId?.let { availableAccounts[it] }

                        val requestDone = run requestSync@ {
                            account ?: run {
                                Log.w(LOG_TAG, "onUpdate()" +
                                        " -> Failed to request sync of '$accountId'" +
                                        " -> Account not found")
                                return@requestSync false
                            }

                            if (!GradesLoginData.loginData.isLoggedIn(accountId)) {
                                Log.w(LOG_TAG, "onUpdate()" +
                                        " -> Failed to request sync of '$account'" +
                                        " with id '$accountId'" +
                                        " -> Account is not logged in")
                                return@requestSync false
                            }

                            requestSync(accountId, account)
                            return@requestSync true
                        }

                        if (!requestDone) accountAppWidgetIds.forEach {
                            showContent(context, appWidgetManager, it, accountId, account)
                        }
                    }
        } else {
            // Update all widgets
            appWidgetIdsMap.forEach {
                val (appWidgetId, accountId) = it
                val account = accountId?.let { availableAccounts[it] }

                showContent(context, appWidgetManager, appWidgetId, accountId, account)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preferences associated with it.
        val prefs = GradesPreferences.instance
        appWidgetIds.forEach {
            prefs.removeAppWidgetAccountId(it)
            prefs.removeAppWidgetSort(it)
        }
    }
}


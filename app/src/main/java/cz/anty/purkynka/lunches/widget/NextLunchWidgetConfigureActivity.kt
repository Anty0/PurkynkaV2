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

package cz.anty.purkynka.lunches.widget

import android.accounts.Account
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle

import cz.anty.purkynka.R
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.account.ui.AccountSpinnerItem
import cz.anty.purkynka.lunches.save.LunchesPreferences
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.to
import eu.codetopic.utils.edit
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.receiver
import eu.codetopic.utils.sendSuspendOrderedBroadcast
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.CoordinatorLayoutModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.container.adapter.CustomItemAdapter
import eu.codetopic.utils.ui.container.adapter.forSpinner
import eu.codetopic.utils.ui.view.holder.loading.LoadingModularActivity
import kotlinx.android.synthetic.main.activity_widget_next_lunch_configure.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.design.longSnackbar

/**
 * The configuration screen for the [NextLunchWidgetProvider] AppWidget.
 */
class NextLunchWidgetConfigureActivity : LoadingModularActivity(
        CoordinatorLayoutModule(), ToolbarModule(), BackButtonModule()
) {

    companion object {

        private const val LOG_TAG = "NextLunchWidgetConfigureActivity"
    }

    private val updateReceiver = receiver { _, _ -> update() }

    private var accountsAdapter: CustomItemAdapter<AccountSpinnerItem>? = null

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private var accountsMap: Map<String, Account>? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        setContentView(R.layout.activity_widget_next_lunch_configure)

        // Get the widget id from the intent.
        appWidgetId = intent?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(LOG_TAG, "onCreate()" +
                    " -> Failed to configure widget" +
                    " -> Widget id not found in intent")
            finish()
            return
        }

        accountsAdapter = CustomItemAdapter(this)

        boxAccountsSpinner.adapter = accountsAdapter?.forSpinner()

        butAddWidget.setOnClickListener click@ {
            val appWidgetId = appWidgetId
            val accountId = boxAccountsSpinner.selectedItem.to<AccountSpinnerItem>()?.accountId
                    ?: run {
                        longSnackbar(
                                view = butAddWidget,
                                message = R.string.snackbar_next_lunch_widget_add_fail_no_account
                        )
                        return@click
                    }

            val holder = holder
            val self = this@NextLunchWidgetConfigureActivity.asReference()
            val appContext = applicationContext
            launch(UI) {
                holder.showLoading()

                LunchesPreferences.instance.apply {
                    setAppWidgetAccountId(appWidgetId, accountId)
                }

                delay(500) // Wait for preferences update

                // It is the responsibility of the configuration activity
                //  to update the app widget
                appContext.sendSuspendOrderedBroadcast(
                        NextLunchWidgetProvider
                                .getUpdateIntent(appContext, intArrayOf(appWidgetId))
                                .also {
                                    if (Build.VERSION.SDK_INT >= 16)
                                        it.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                }
                )

                self().setResult(
                        RESULT_OK,
                        Intent().putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                appWidgetId
                        )
                )

                delay(500) // Wait for widget update

                self().finish()

                holder.hideLoading()
            }
        }

        updateWithLoading()
    }

    override fun onStart() {
        super.onStart()

        register()
    }

    override fun onStop() {
        unregister()

        super.onStop()
    }

    private fun register(): Job {
        registerReceiver(
                updateReceiver,
                intentFilter(Accounts.ACTION_ACCOUNTS_CHANGED)
        )

        return update()
    }

    private fun unregister() {
        unregisterReceiver(updateReceiver)
    }

    private fun updateWithLoading(): Job {
        val holder = holder
        val job = update()
        return launch(UI) {
            holder.showLoading()

            job.join()

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }

    private fun update(): Job {
        val appContext = applicationContext
        val self = this.asReference()
        return launch(UI) {
            self().accountsMap = Accounts.getAllWIthIds(appContext)

            self().updateUi()
        }
    }

    private fun updateUi() {
        accountsAdapter?.edit {
            clear()
            accountsMap?.map { AccountSpinnerItem(it.value, it.key) }
                    ?.let { addAll(it) }

            notifyAllItemsChanged()
        }
    }
}
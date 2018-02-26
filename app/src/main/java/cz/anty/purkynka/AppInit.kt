/*
 * app
 * Copyright (C)   2017  anty
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

package cz.anty.purkynka

import android.content.Context
import android.support.multidex.MultiDexApplication
import android.support.v4.content.ContextCompat
import com.crashlytics.android.Crashlytics
import com.evernote.android.job.JobManager
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.Iconics
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.exceptions.LoggedException
import cz.anty.purkynka.feedback.save.FeedbackData
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel
import cz.anty.purkynka.grades.widget.GradesWidgetUpdateReceiver
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesPreferences
import cz.anty.purkynka.grades.save.GradesUiData
import cz.anty.purkynka.grades.sync.GradesSyncAdapter
import cz.anty.purkynka.grades.widget.GradesWidgetProvider
import cz.anty.purkynka.lunches.notify.LunchesBurzaWatcherResultChannel
import cz.anty.purkynka.lunches.notify.LunchesBurzaWatcherStatusChannel
import cz.anty.purkynka.lunches.notify.LunchesBurzaWatcherStatusGroup
import cz.anty.purkynka.lunches.notify.LunchesChangesNotifyChannel
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.lunches.save.LunchesPreferences
import cz.anty.purkynka.lunches.sync.LunchesSyncAdapter
import cz.anty.purkynka.update.notify.UpdateNotifyChannel
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.update.notify.VersionChangesNotifyChannel
import cz.anty.purkynka.update.sync.UpdateCheckJob
import cz.anty.purkynka.update.sync.UpdateCheckJobCreator
import cz.anty.purkynka.update.save.UpdateData
import cz.anty.purkynka.utils.EvernoteJobManagerExtension
import cz.anty.purkynka.wifilogin.save.WifiData
import cz.anty.purkynka.wifilogin.save.WifiLoginData
import eu.codetopic.utils.ui.container.recycler.RecyclerInflater
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.log.Logger
import eu.codetopic.java.utils.log.base.Priority
import eu.codetopic.utils.UtilsBase
import eu.codetopic.utils.UtilsBase.PARAM_INITIALIZE_UTILS
import eu.codetopic.utils.UtilsBase.processNamePrimary
import eu.codetopic.utils.UtilsBase.processNameProviders
import eu.codetopic.utils.UtilsBase.processNameNotifyManager
import eu.codetopic.utils.broadcast.BroadcastsConnector
import eu.codetopic.utils.notifications.manager.NotifyManager
import io.fabric.sdk.android.Fabric
import org.jetbrains.anko.bundleOf


/**
 * Created by anty on 10/7/17.
 * @author anty
 */
class AppInit : MultiDexApplication() {

    companion object {

        const val PROCESS_NAME_SYNCS = ":syncs"
        const val PROCESS_NAME_WIDGETS = ":widgets"
        const val PROCESS_NAME_ACCOUNTS = ":accounts"

        val Context.processNameSyncs: String
            get() = processNamePrimary + PROCESS_NAME_SYNCS

        val Context.processNameAccounts: String
            get() = processNamePrimary + PROCESS_NAME_ACCOUNTS

        val Context.processNameWidgets: String
            get() = processNamePrimary + PROCESS_NAME_WIDGETS
     }

    override fun onCreate() {
        super.onCreate()

        // Prepare utils and processes params
        UtilsBase.prepare(this) {
            addParams(
                    // Primary process
                    processNamePrimary to bundleOf(
                            PARAM_INITIALIZE_UTILS to true
                    ),
                    // Data management process (multi-process data access support)
                    processNameProviders to bundleOf(
                            PARAM_INITIALIZE_UTILS to true
                    ),
                    // Data synchronization process
                    processNameSyncs to bundleOf(
                            PARAM_INITIALIZE_UTILS to true
                    ),
                    // Widgets process (process where all widgets are being updated)
                    processNameWidgets to bundleOf(
                            PARAM_INITIALIZE_UTILS to true
                    ),
                    // NotifyManager's process
                    processNameNotifyManager to bundleOf(
                            PARAM_INITIALIZE_UTILS to true
                    ),
                    // AuthenticatorService's process
                    processNameAccounts to bundleOf(
                            PARAM_INITIALIZE_UTILS to true
                    )
            )
        }

        val isInSelfProcess = UtilsBase.Process.name in arrayOf(
                processNamePrimary,
                processNameProviders,
                processNameSyncs,
                processNameWidgets,
                processNameNotifyManager,
                processNameAccounts
        )

        if (isInSelfProcess) {
            // Initialize stuff, that should be initialized before anything else
            initAllProcessesBeforeUtils()
        }

        // Initialize utils base (my own android application framework; brain of this application)
        UtilsBase.initialize(this) { processName, _ ->
            if (isInSelfProcess) initAllProcesses()

            when (processName) {
                processNamePrimary -> initProcessPrimary()
                processNameProviders -> initProcessProviders()
                processNameSyncs -> initProcessSyncs()
                processNameWidgets -> initProcessWidgets()
                processNameNotifyManager -> initProcessNotifyManager()
                processNameAccounts -> initProcessAccounts()
            }
        }


        if (isInSelfProcess) postInitAllProcesses()
    }

    private fun initAllProcessesBeforeUtils() {
        // Setup crashlytics
        Fabric.with(this, Crashlytics())

        // Setup uncaught exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            Log.d("UExHandler", "Oh no, something went wrong (uncaught exception). " +
                    "Ok, let's enable Feedback module...")

            FeedbackData.takeIf { it.isInitialized() }
                    ?.instance?.notifyErrorReceived()

            defaultHandler.uncaughtException(thread, ex)
        }

        // Setup error logged listener
        Logger.logsHandler.addOnLoggedListener onLogged@ {
            if (it.priority != Priority.ERROR) return@onLogged

            Log.d("UExHandler", "Oh no, something went wrong (error logged). " +
                    "Ok, let's enable Feedback module...")

            Crashlytics.getInstance()?.core
                    ?.logException(LoggedException(it))

            FeedbackData.takeIf { it.isInitialized() }
                    ?.instance?.notifyErrorReceived()
        }

        // Setup Iconics
        Iconics.registerFont(GoogleMaterial())
        Iconics.registerFont(CommunityMaterial())
        Iconics.markInitDone()
    }

    private fun initAllProcesses() {
        // Let's initialize FeedbackData first, as they plays important role in error handling.
        FeedbackData.initialize(this)

        // Initialize accounts (create default account, initialize accounts channels, etc.)
        initAccounts()

        // Init notifications channels (prepare them for NotifyManager)
        initNotifyManager()
    }

    private fun postInitAllProcesses() {
        // Set color scheme of loading in RecyclerView
        RecyclerInflater.setDefaultSwipeSchemeColors(
                ContextCompat.getColor(this, R.color.colorPrimary),
                ContextCompat.getColor(this, R.color.colorPrimaryGrades),
                ContextCompat.getColor(this, R.color.colorPrimaryAttendance),
                ContextCompat.getColor(this, R.color.colorPrimaryLunches),
                ContextCompat.getColor(this, R.color.colorPrimaryWifiLogin)
        )
    }

    private fun initProcessPrimary() {
        // Initialize data providers required in this process
        UpdateData.initialize(this)
        GradesUiData.initialize(this)
        GradesData.initialize(this)
        GradesLoginData.initialize(this)
        GradesPreferences.initialize(this)
        WifiData.initialize(this)
        WifiLoginData.initialize(this)
        LunchesData.initialize(this)
        LunchesLoginData.initialize(this)
        LunchesPreferences.initialize(this)

        // Init Evernote's JobManager used here for app updates checking
        initJobManager()

        // Request init of Evernote's JobManager used here for app updates checking
        //requestInitJobManager()

        // Initialize sync adapters
        GradesSyncAdapter.init(this)
        LunchesSyncAdapter.init(this)
    }

    private fun initProcessProviders() {
        // Initialize data providers required in this process
        GradesData.initialize(this)
        GradesLoginData.initialize(this)
        GradesPreferences.initialize(this)

        // When notify data is updated, update widgets
        BroadcastsConnector.connect(
                NotifyManager.getOnChangeBroadcastAction(),
                BroadcastsConnector.Connection(
                        BroadcastsConnector.BroadcastTargetingType.GLOBAL,
                        GradesWidgetUpdateReceiver.ACTION_WIDGET_UPDATE_ITEMS
                )
        )

        // When grades data is updated, update grades widgets
        BroadcastsConnector.connect(
                GradesData.instance.broadcastActionChanged,
                BroadcastsConnector.Connection(
                        BroadcastsConnector.BroadcastTargetingType.ORDERED_GLOBAL,
                        GradesWidgetUpdateReceiver.ACTION_WIDGET_UPDATE_ITEMS
                )
        )
        BroadcastsConnector.connect(
                GradesLoginData.instance.broadcastActionChanged,
                BroadcastsConnector.Connection(
                        BroadcastsConnector.BroadcastTargetingType.GLOBAL,
                        GradesWidgetProvider.getUpdateIntent(this)
                )
        )
        BroadcastsConnector.connect(
                GradesPreferences.instance.broadcastActionChanged,
                BroadcastsConnector.Connection(
                        BroadcastsConnector.BroadcastTargetingType.GLOBAL,
                        GradesWidgetProvider.getUpdateIntent(this)
                )
        )
    }

    private fun initProcessSyncs() {
        // Initialize data providers required in this process
        UpdateData.initialize(this)
        GradesData.initialize(this)
        GradesLoginData.initialize(this)
        WifiData.initialize(this)
        WifiLoginData.initialize(this)
        LunchesData.initialize(this)
        LunchesLoginData.initialize(this)
    }

    private fun initProcessWidgets() {
        // Initialize data providers required in this process
        GradesData.initialize(this)
        GradesLoginData.initialize(this)
        GradesPreferences.initialize(this)
    }

    private fun initProcessNotifyManager() {
        // Initialize data providers required in this process
        //  No data providers to initialize in this process
    }

    private fun initProcessAccounts() {
        // Initialize data providers required in this process
        //  No data providers to initialize in this process
    }

    private fun initJobManager() {
        EvernoteJobManagerExtension.install()

        JobManager.create(this)
                .addJobCreator(UpdateCheckJobCreator())

        UpdateCheckJob.schedule()
    }

    private fun initAccounts() {
        Accounts.initialize(
                this,
                GradesChangesNotifyChannel.ID,
                LunchesBurzaWatcherResultChannel.ID,
                LunchesChangesNotifyChannel.ID
        )
    }

    private fun initNotifyManager() {
        NotifyManager.installGroups(
                this,
                UpdateNotifyGroup(),
                LunchesBurzaWatcherStatusGroup()
        )
        NotifyManager.installChannels(
                this,
                UpdateNotifyChannel(),
                VersionChangesNotifyChannel(),
                GradesChangesNotifyChannel(),
                LunchesBurzaWatcherStatusChannel(),
                LunchesBurzaWatcherResultChannel(),
                LunchesChangesNotifyChannel()
        )
    }
}
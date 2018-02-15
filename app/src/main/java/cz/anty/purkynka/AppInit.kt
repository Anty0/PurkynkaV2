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
import com.evernote.android.job.JobManager
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.grades.notify.GradesChangesNotifyChannel
import cz.anty.purkynka.grades.receiver.UpdateGradesSyncReceiver
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesUiData
import cz.anty.purkynka.grades.sync.GradesSyncAdapter
import cz.anty.purkynka.lunches.notify.LunchesBurzaWatcherResultChannel
import cz.anty.purkynka.lunches.notify.LunchesBurzaWatcherStatusChannel
import cz.anty.purkynka.lunches.notify.LunchesBurzaWatcherStatusGroup
import cz.anty.purkynka.lunches.receiver.UpdateLunchesSyncReceiver
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.lunches.sync.LunchesSyncAdapter
import cz.anty.purkynka.settings.SettingsData
import cz.anty.purkynka.update.notify.UpdateNotifyChannel
import cz.anty.purkynka.update.notify.UpdateNotifyGroup
import cz.anty.purkynka.update.sync.UpdateCheckJob
import cz.anty.purkynka.update.sync.UpdateCheckJobCreator
import cz.anty.purkynka.update.save.UpdateData
import cz.anty.purkynka.utils.EvernoteJobManagerExtension
import cz.anty.purkynka.wifilogin.save.WifiData
import cz.anty.purkynka.wifilogin.save.WifiLoginData
import eu.codetopic.utils.ui.container.recycler.RecyclerInflater
import eu.codetopic.utils.ui.container.adapter.dashboard.DashboardData
import eu.codetopic.java.utils.log.base.LogLine
import eu.codetopic.java.utils.log.LogsHandler
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
import org.jetbrains.anko.bundleOf


/**
 * Created by anty on 10/7/17.
 * @author anty
 */
class AppInit : MultiDexApplication() {

    companion object {

        const val PROCESS_NAME_SYNCS = ":syncs"
        const val PROCESS_NAME_ACCOUNTS = ":accounts"

        val Context.processNameSyncs: String
            get() = processNamePrimary + PROCESS_NAME_SYNCS

        val Context.processNameAccounts: String
            get() = processNamePrimary + PROCESS_NAME_ACCOUNTS
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
                processNameNotifyManager -> initProcessNotifyManager()
                processNameAccounts -> initProcessAccounts()
            }
        }


        if (isInSelfProcess) postInitAllProcesses()
    }

    private fun initAllProcessesBeforeUtils() {
        // Setup uncaught exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            Log.d("UExHandler", "Oh no, something went wrong (uncaught exception). " +
                    "Ok, let's enable Feedback module...")
            // TODO: 6/16/17 enable feedback module
            defaultHandler.uncaughtException(thread, ex)
        }

        // Setup error logged listener
        Logger.logsHandler.addOnLoggedListener(object : LogsHandler.OnLoggedListener {
            override fun onLogged(logLine: LogLine) {
                Log.d("UExHandler", "Oh no, something went wrong (error logged). " +
                        "Ok, let's enable Feedback module...")
                // TODO: 6/16/17 enable feedback module
            }

            override val filterPriorities: Array<Priority>?
                get() = arrayOf(Priority.ERROR)
        })
    }

    private fun initAllProcesses() {
        // Prepare broadcasts connections (very helpful tool)
        initBroadcastConnections()

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
        MainData.initialize(this)
        UpdateData.initialize(this)
        SettingsData.initialize(this)
        GradesUiData.initialize(this)
        GradesData.initialize(this)
        GradesLoginData.initialize(this)
        WifiData.initialize(this)
        WifiLoginData.initialize(this)
        LunchesData.initialize(this)
        LunchesLoginData.initialize(this)

        // Initialize data provider of dashboard framework
        DashboardData.initialize(this)

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
        //  Nothing to initialize in this process
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

    private fun initProcessNotifyManager() {
        // Initialize data providers required in this process
        //  Nothing to initialize in this process
    }

    private fun initProcessAccounts() {
        // Initialize data providers required in this process
        //  Nothing to initialize in this process
    }

    private fun initBroadcastConnections() {
        BroadcastsConnector.connect(
                Accounts.ACTION_ACCOUNT_ADDED,
                BroadcastsConnector.Connection(
                        BroadcastsConnector.BroadcastTargetingType.GLOBAL,
                        UpdateGradesSyncReceiver.getIntent(this)
                )
        )
        BroadcastsConnector.connect(
                Accounts.ACTION_ACCOUNT_ADDED,
                BroadcastsConnector.Connection(
                        BroadcastsConnector.BroadcastTargetingType.GLOBAL,
                        UpdateLunchesSyncReceiver.getIntent(this)
                )
        )
    }

    private fun initJobManager() {
        EvernoteJobManagerExtension.install()

        JobManager.create(this)
                .addJobCreator(UpdateCheckJobCreator())

        UpdateCheckJob.schedule()
    }

    /*private fun requestInitJobManager() {
        UpdateCheckJob.requestSchedule(this)
    }*/

    private fun initAccounts() {
        Accounts.initialize(
                this,
                GradesChangesNotifyChannel.ID,
                LunchesBurzaWatcherResultChannel.ID
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
                GradesChangesNotifyChannel(),
                LunchesBurzaWatcherStatusChannel(),
                LunchesBurzaWatcherResultChannel()
        )
    }
}
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

import android.app.Application
import android.support.multidex.MultiDexApplication
import android.support.v4.content.ContextCompat
import com.evernote.android.job.JobConfig
import com.evernote.android.job.JobManager
import cz.anty.purkynka.account.Accounts
import cz.anty.purkynka.grades.notify.GradesChangesNotificationGroup
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesLoginData
import cz.anty.purkynka.grades.save.GradesUiData
import cz.anty.purkynka.grades.sync.GradesSyncAdapter
import cz.anty.purkynka.settings.SettingsData
import cz.anty.purkynka.update.UpdateCheckJob
import cz.anty.purkynka.update.UpdateCheckJobCreator
import cz.anty.purkynka.update.UpdateData
import cz.anty.purkynka.wifilogin.save.WifiLoginData
import eu.codetopic.utils.ui.container.recycler.RecyclerInflater
import eu.codetopic.utils.ui.container.adapter.dashboard.DashboardData
import eu.codetopic.java.utils.log.base.LogLine
import eu.codetopic.java.utils.log.LogsHandler
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.log.Logger
import eu.codetopic.java.utils.log.base.Priority
import eu.codetopic.utils.UtilsBase.ProcessProfile
import eu.codetopic.utils.UtilsBase
import eu.codetopic.utils.broadcast.BroadcastsConnector
import eu.codetopic.utils.notifications.manager.NotificationsManager


/**
 * Created by anty on 10/7/17.
 * @author anty
 */
class AppInit : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        // Initialize stuff, that should be initialized before anything else
        initAllProcesses()

        // Initialize utils base (my own android application framework; brain of this application)
        UtilsBase.initialize(this,
                ProcessProfile(packageName, true,
                        Runnable(::initProcessPrimary)), // Primary process
                ProcessProfile("$packageName:providers", true,
                        Runnable(::initProcessProviders)), // Data management process (multi-process data access support)
                ProcessProfile("$packageName:syncs", true,
                        Runnable(::initProcessSyncs)) // Data synchronization process
                // ProcessProfile(app.packageName + ":acra", DISABLE_UTILS), // ACRA reporting process
        )
    }

    private fun initAllProcesses() {
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

        // Set color scheme of loading in RecyclerView
        RecyclerInflater.setDefaultSwipeSchemeColors(
                ContextCompat.getColor(this, R.color.colorPrimary),
                ContextCompat.getColor(this, R.color.colorPrimaryGrades),
                ContextCompat.getColor(this, R.color.colorPrimaryAttendance),
                ContextCompat.getColor(this, R.color.colorPrimaryLunches),
                ContextCompat.getColor(this, R.color.colorPrimaryTimetables),
                ContextCompat.getColor(this, R.color.colorPrimaryWifiLogin)
        )
    }

    private fun initProcessPrimary() {
        // Prepare broadcasts connections (very helpful tool)
        initBroadcastConnections()

        // Initialize accounts (create default account, initialize accounts channels, etc.)
        Accounts.initialize(this)

        // Initialize data providers required in this process
        MainData.initialize(this)
        UpdateData.initialize(this)
        SettingsData.initialize(this)
        GradesUiData.initialize(this)
        GradesData.initialize(this)
        GradesLoginData.initialize(this)
        WifiLoginData.initialize(this)

        // Initialize data provider of dashboard framework
        DashboardData.initialize(this)

        // Init notifications groups (prepare them for NotificationsManager)
        NotificationsManager.initGroups(
                this,
                GradesChangesNotificationGroup()
        )

        initJobManager()

        // Initialize sync adapters
        GradesSyncAdapter.init(this)
    }

    private fun initProcessProviders() {
        // Prepare broadcasts connections (very helpful tool)
        initBroadcastConnections()
    }

    private fun initProcessSyncs() {
        // Prepare broadcasts connections (very helpful tool)
        initBroadcastConnections()

        // Initialize data providers required in this process
        GradesData.initialize(this)
        GradesLoginData.initialize(this)
        //GradesDataDifferences.initialize(this)
    }

    private fun initBroadcastConnections() {
        // TODO: 6/16/17 initialize broadcast connections
    }

    private fun initJobManager() {
        // JobConfig.addLogger() // TODO: add own logger to JobConfig
        // JobConfig.setLogcatEnabled(false)

        JobManager.create(this) // TODO: add here job creators
                .addJobCreator(UpdateCheckJobCreator())

        UpdateCheckJob.schedule()
    }
}
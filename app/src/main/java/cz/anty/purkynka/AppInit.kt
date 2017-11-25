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
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.accounts.AccountsHelper
import cz.anty.purkynka.accounts.ActiveAccountManager
import cz.anty.purkynka.grades.save.GradesData
import cz.anty.purkynka.grades.save.GradesSyncAdapter
import cz.anty.purkynka.settings.SettingsData
import eu.codetopic.utils.ui.container.recycler.RecyclerInflater
import eu.codetopic.utils.timing.TimedComponentsManager
import eu.codetopic.utils.ui.container.adapter.dashboard.DashboardData
import eu.codetopic.java.utils.log.base.LogLine
import eu.codetopic.java.utils.log.LogsHandler
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.log.Logger
import eu.codetopic.java.utils.log.base.Priority
import eu.codetopic.utils.UtilsBase.ProcessProfile
import eu.codetopic.utils.UtilsBase

import eu.codetopic.utils.UtilsBase.InitType.PRIMARY_PROCESS
import eu.codetopic.utils.UtilsBase.InitType.ANOTHER_PROCESS
import eu.codetopic.utils.thread.job.SingletonJobManager


/**
 * Created by anty on 10/7/17.
 * @author anty
 */
class AppInit : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize stuff, that should be initialized before anything else
        initAllProcesses()

        // Initialize utils base (my own android application framework; brain of this application)
        UtilsBase.initialize(this,
                ProcessProfile(packageName, PRIMARY_PROCESS, Runnable { initPrimaryProcess() }), // Primary process // TODO: use "callable references to class members with empty l h s" in kotlin 1.2
                ProcessProfile("$packageName:providers", ANOTHER_PROCESS, Runnable { initAnotherProcess() }), // Data management process (multi-process data access support)  // TODO: use "callable references to class members with empty l h s" in kotlin 1.2
                ProcessProfile("$packageName:syncs", ANOTHER_PROCESS, Runnable { initAnotherProcess() }) // Data synchronization process  // TODO: use "callable references to class members with empty l h s" in kotlin 1.2
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
        Logger.getErrorLogsHandler().addOnLoggedListener(object : LogsHandler.OnLoggedListener {
            override fun onLogged(logLine: LogLine) {
                Log.d("UExHandler", "Oh no, something went wrong (error logged). " +
                        "Ok, let's enable Feedback module...")
                // TODO: 6/16/17 enable feedback module
            }

            override fun filterPriorities() = arrayOf(Priority.ERROR)
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

    private fun initPrimaryProcess() {

        // Create default user account (if there is none)
        AccountsHelper.initDefaultAccount(this)

        // Prepare broadcasts connections (very helpful tool)
        loadBroadcastConnections()

        // Initialize sync adapters
        GradesSyncAdapter.init(this)

        // Initialize data app specific providers
        MainData.initialize(this)
        ActiveAccountManager.initialize(this)
        SettingsData.initialize(this)
        GradesData.initialize(this)

        // Initialize data provider of dashboard framework
        DashboardData.initialize(this)

        // Initialize singletons
        SingletonJobManager.initialize(this)
        //SingletonDatabase.initialize(new AppDatabase(app))

        // Initialize timed components (framework for repeating jobs)
        /*TimedComponentsManager.initialize(app,
                SettingsData.getter.get().requiredNetworkType,
                        GradesSyncService::class.java)*/

    }

    private fun initAnotherProcess() {
        // Prepare broadcasts connections (very helpful tool)
        loadBroadcastConnections()

        // Initialize sync adapters
        GradesSyncAdapter.init(this)
    }

    private fun loadBroadcastConnections() {
        // TODO: 6/16/17 initialize broadcast connections
    }
}
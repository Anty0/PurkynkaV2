package cz.anty.purkynka

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Application
import android.support.v4.content.ContextCompat
import cz.anty.purkynka.accounts.AccountsHelper
import cz.anty.purkynka.accounts.ActiveAccountManager
import cz.anty.purkynka.marks.MarksData
import cz.anty.purkynka.marks.MarksLoginData
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

import eu.codetopic.utils.UtilsBase.InitType.DISABLE_UTILS
import eu.codetopic.utils.UtilsBase.InitType.INIT_NORMAL_MODE
import eu.codetopic.utils.thread.job.SingletonJobManager


/**
 * Created by anty on 10/7/17.
 * @author anty
 */
class AppInit : Application() {

    override fun onCreate() {
        super.onCreate()
        RecyclerInflater.setDefaultSwipeSchemeColors(
                ContextCompat.getColor(this, R.color.colorPrimary),
                ContextCompat.getColor(this, R.color.colorPrimaryMarks),
                ContextCompat.getColor(this, R.color.colorPrimaryAttendance),
                ContextCompat.getColor(this, R.color.colorPrimaryLunches),
                ContextCompat.getColor(this, R.color.colorPrimaryTimetables),
                ContextCompat.getColor(this, R.color.colorPrimaryWifiLogin)
        )

        val app = this

        UtilsBase.initialize(app,
                // ProcessProfile(app.packageName + ":acra", DISABLE_UTILS), // ACRA reporting process
                ProcessProfile(app.packageName, INIT_NORMAL_MODE, Runnable {
                    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                    Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
                        Log.d("UExHandler", "Enabling Feedback Module...")
                        // TODO: 6/16/17 enable feedback module
                        defaultHandler.uncaughtException(thread, ex)
                    }

                    Logger.getErrorLogsHandler().addOnLoggedListener(object : LogsHandler.OnLoggedListener {
                        override fun onLogged(logLine: LogLine) {
                            Log.d("UExHandler", "Enabling Feedback Module...")
                            // TODO: 6/16/17 enable feedback module
                        }

                        override fun filterPriorities(): Array<Priority> {
                            return arrayOf(Priority.ERROR)
                        }
                    })

                    AccountsHelper.initDefaultAccount(app)

                    loadBroadcastConnections()

                    MainData.initialize(app)
                    ActiveAccountManager.initialize(app)
                    SettingsData.initialize(app)
                    MarksData.initialize(app)
                    MarksLoginData.initialize(app)
                    DashboardData.initialize(app)

                    SingletonJobManager.initialize(app)
                    //SingletonDatabase.initialize(new AppDatabase(app))

                    TimedComponentsManager.initialize(app, SettingsData.getter.get()
                            .requiredNetworkType/*, MarksService::class.java*/)
                }))
    }

    private fun loadBroadcastConnections() {
        // TODO: 6/16/17 initialize broadcast connections
    }
}
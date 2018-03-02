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

package cz.anty.purkynka.utils

import android.content.Context
import com.crashlytics.android.Crashlytics
import cz.anty.purkynka.exceptions.LoggedException
import cz.anty.purkynka.feedback.save.FeedbackData
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.log.Logger
import eu.codetopic.java.utils.log.base.LogLine
import eu.codetopic.java.utils.log.base.Priority

/**
 * @author anty
 */
object ExceptionHandlingExtension {

    private lateinit var defaultUncaughtHandler: Thread.UncaughtExceptionHandler
    private val uncaughtHandler = Thread.UncaughtExceptionHandler { thread, ex ->
        Log.d("UExHandler", "Oh no, something went wrong (uncaught exception). " +
                "Ok, let's enable Feedback module...")

        FeedbackData.takeIf { it.isInitialized() }
                ?.instance?.notifyErrorReceived()

        defaultUncaughtHandler.uncaughtException(thread, ex)
    }
    private val loggedHandler: (LogLine) -> Unit = onLogged@ {
        if (it.priority != Priority.ERROR) return@onLogged

        Log.d("UExHandler", "Oh no, something went wrong (error logged). " +
                "Ok, let's enable Feedback module...")

        Crashlytics.getInstance()?.core
                ?.logException(LoggedException(it))

        FeedbackData.takeIf { it.isInitialized() }
                ?.instance?.notifyErrorReceived()
    }

    @Suppress("UNUSED_PARAMETER")
    fun install(context: Context) {
        // Setup uncaught exception handler
        defaultUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(uncaughtHandler)

        // Setup error logged listener
        Logger.logsHandler.addOnLoggedListener(loggedHandler)
    }
}
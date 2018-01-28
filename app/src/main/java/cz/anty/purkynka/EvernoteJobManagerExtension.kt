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

package cz.anty.purkynka

import com.evernote.android.job.JobConfig
import com.evernote.android.job.util.JobLogger
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.log.base.LogLine
import eu.codetopic.java.utils.log.base.Priority

/**
 * @author anty
 */
object EvernoteJobManagerExtension {

    fun install() {
        JobConfig.addLogger(JobLogTarget())
        JobConfig.setLogcatEnabled(false)
    }

    class JobLogTarget : JobLogger {

        override fun log(priority: Int, tag: String, message: String, t: Throwable?) {
            Log.println(LogLine(priority.toPriority(), tag, message, t))
        }

        private fun Int.toPriority(): Priority {
            return when (this) {
                android.util.Log.ASSERT -> Priority.ASSERT
                android.util.Log.ERROR -> Priority.ERROR
                android.util.Log.WARN -> Priority.WARN
                android.util.Log.INFO -> Priority.INFO
                android.util.Log.DEBUG -> Priority.DEBUG
                android.util.Log.VERBOSE -> Priority.VERBOSE
                else -> Priority.DEBUG
            }
        }
    }
}
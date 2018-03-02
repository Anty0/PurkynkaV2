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

package cz.anty.purkynka.update.load

import android.content.Context
import android.net.Uri
import android.os.Build
import android.support.annotation.WorkerThread
import cz.anty.purkynka.utils.*
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.debug.DebugMode
import org.jsoup.Jsoup
import android.support.v4.content.FileProvider
import cz.anty.purkynka.update.data.AvailableVersionInfo
import eu.codetopic.java.utils.letIfNull
import eu.codetopic.java.utils.to
import eu.codetopic.utils.thread.progress.ProgressReporter
import kotlinx.serialization.json.JSON
import java.io.*
import java.math.BigInteger
import java.net.URL
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.net.ssl.HttpsURLConnection


/**
 * @author anty
 */
object UpdateFetcher {

    private const val LOG_TAG = "UpdateFetcher"

    private val API_VERSION = if (DebugMode.isEnabled) "dev" else "v1"
    private val URL_BASE = "https://anty.codetopic.eu/purkynka/api/$API_VERSION"
    private val URL_VERSION = "$URL_BASE/getAvailableVersionInfo.php"

    private const val DIR_UPDATES = "updates"

    private fun getApkNameFor(versionInfo: AvailableVersionInfo): String =
            "app-v${versionInfo.code}.apk"

    private fun getApksDir(context: Context): File =
            File(context.externalCacheDir, DIR_UPDATES)

    fun getApkFileFor(context: Context, versionInfo: AvailableVersionInfo): File =
            File(context.externalCacheDir, "$DIR_UPDATES/${getApkNameFor(versionInfo)}")

    fun getApkUriFor(context: Context, versionInfo: AvailableVersionInfo): Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                        context,
                        AUTHORITY_PROVIDER_FILES,
                        getApkFileFor(context, versionInfo)
                )
            } else {
                Uri.fromFile(getApkFileFor(context, versionInfo))
            }

    @WorkerThread
    fun fetchVersionInfo(): AvailableVersionInfo? = try {
        val json = Jsoup.connect(URL_VERSION)
                .userAgent(userAgent)
                .ignoreContentType(true)
                .followRedirects(false) // basic protection against login pages
                .execute().body()
        JSON.parse<AvailableVersionInfo>(json)
    } catch (e: Exception) {
        Log.w(LOG_TAG, "fetchVersionInfo()", e); null
    }.also {
        Log.d(LOG_TAG, "fetchVersionInfo() -> (versionInfo=$it)")
    }

    @WorkerThread
    fun checkApk(appContext: Context, versionInfo: AvailableVersionInfo): Boolean {
        val sha512sum = versionInfo.sha512sum
        val apkFile = getApkFileFor(appContext, versionInfo)

        if (sha512sum.isNullOrEmpty()) {
            Log.d(LOG_TAG, "checkApk()" +
                    " -> Can't check apk" +
                    " -> SHA-512 string empty or null")
            return false
        }


        if (!apkFile.isFile) {
            Log.d(LOG_TAG, "checkApk()" +
                    " -> Can't check apk" +
                    " -> no apk file found")
            return false
        }

        val apkSha512sum = run calcSHA@ {
            try {
                val digest = MessageDigest.getInstance("SHA-512")

                FileInputStream(apkFile).use {
                    it.copyTo(DigestOutputStream(digest))
                }

                val sha512sumApkBArray = digest.digest()
                val sha512sumApkStr = BigInteger(1, sha512sumApkBArray).toString(16)
                return@calcSHA "%32s".format(sha512sumApkStr).replace(' ', '0')
            } catch (e: NoSuchAlgorithmException) {
                Log.w(LOG_TAG, "checkApk()" +
                        " -> Failed to check apk", e)
                return@calcSHA null
            }
        } ?: return false

        Log.v(LOG_TAG, "checkApk() -> (calculatedSum=$apkSha512sum)")
        Log.v(LOG_TAG, "checkApk() -> (targetSum=$sha512sum)")

        return apkSha512sum.equals(sha512sum, ignoreCase = true)
    }

    @WorkerThread
    fun removeApk(appContext: Context, versionInfo: AvailableVersionInfo) {
        val apkFile = getApkFileFor(appContext, versionInfo)

        try {
            if (!apkFile.isFile) return
            apkFile.delete()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "removeApk()", e)
        }
    }

    @WorkerThread
    fun cleanupApksDir(appContext: Context, ignoredVersionInfo: AvailableVersionInfo) {
        val apksDir = getApksDir(appContext)
        val ignoredApkFile = getApkFileFor(appContext, ignoredVersionInfo)

        try {
            if (!apksDir.isDirectory) return
            apksDir.listFiles { it -> it != ignoredApkFile }.forEach {
                try {
                    it.delete()
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "cleanupApksDir() -> Remove apk", e)
                }
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "cleanupApksDir()", e)
        }
    }

    @WorkerThread
    fun fetchApk(appContext: Context, versionInfo: AvailableVersionInfo,
                 reporter: ProgressReporter): Boolean {
        try {
            val url = versionInfo.url?.let { "$URL_BASE/$it" } ?: run {
                Log.w(LOG_TAG, "Can't fetch apk -> No download url available")
                return false
            }

            val apkFile = getApkFileFor(appContext, versionInfo)
            if (apkFile.isFile) apkFile.delete()

            getApksDir(appContext).mkdirs()

            val c = URL(url).openConnection()
                    .to<HttpsURLConnection>()
                    .letIfNull {
                        throw RuntimeException("Failed to open connection: invalid connection type")
                    }
                    .apply {
                        requestMethod = "GET"
                        instanceFollowRedirects = false // basic protection against login pages

                        setRequestProperty("User-Agent", userAgent)
                        setRequestProperty("Accept", "*/*")

                        connect()
                    }

            FileOutputStream(apkFile).use { fOut ->
                c.inputStream.use { cIn ->
                    val max = c.contentLength

                    reporter.setMaxProgress(if (max != -1) max else Int.MAX_VALUE)

                    val buffer = ByteArray(1024)
                    var len: Int
                    var completedLen = 0L

                    val currentThread = Thread.currentThread()
                    while (!currentThread.isInterrupted) {
                        len = cIn.read(buffer)
                        if (len == -1) break

                        fOut.write(buffer, 0, len)

                        completedLen += len
                        reporter.reportProgress(
                                if (completedLen > Int.MAX_VALUE) Int.MAX_VALUE
                                else completedLen.toInt()
                        )
                    }

                    if (Thread.interrupted())
                        throw InterruptedException()
                }
            }

            return true
        } catch (e: Exception) {
            Log.w(LOG_TAG, "fetchApk()", e)
            return false
        }
    }

    private class DigestOutputStream(private val digest: MessageDigest) : OutputStream() {

        override fun write(b: Int) = digest.update(b.toByte())

        override fun write(b: ByteArray?) = digest.update(b)

        override fun write(b: ByteArray?, off: Int, len: Int) = digest.update(b, off, len)
    }
}
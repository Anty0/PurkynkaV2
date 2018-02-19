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
import android.support.annotation.WorkerThread
import cz.anty.purkynka.R
import cz.anty.purkynka.utils.*
import eu.codetopic.java.utils.log.Log
import eu.codetopic.java.utils.letIf
import eu.codetopic.java.utils.debug.DebugMode
import eu.codetopic.utils.AndroidUtils
import org.jsoup.Jsoup
import java.io.IOException


/**
 * @author anty
 */
object UpdateFetcher { // TODO: create new/better api

    private const val LOG_TAG = "UpdateFetcher"

    private val API_VERSION = if (DebugMode.isEnabled) "dev" else "v0"

    private val URL_BASE = "https://anty.codetopic.eu/purkynka/api/$API_VERSION"

    private val URL_VERSION_CODE = "$URL_BASE/latestVersionCode"
    private val URL_VERSION_NAME = "$URL_BASE/latestVersionName"
    private val URL_CHANGELOG = "$URL_BASE/latestChangeLog"
    private val URL_APK = "$URL_BASE/latest.apk"

    @WorkerThread
    fun fetchVersionCode(): Int? = try {
        Jsoup.connect(URL_VERSION_CODE)
                .ignoreContentType(true)
                .userAgent(userAgent)
                .followRedirects(false)
                .execute().body().trim().toInt()
    } catch (e: Exception) {
        Log.w(LOG_TAG, "fetchVersionCode()", e); null
    }.also {
        Log.d(LOG_TAG, "fetchVersionCode() -> (versionCode=$it)")
    }

    @WorkerThread
    fun fetchVersionName(): String? = try {
        Jsoup.connect(URL_VERSION_NAME)
                .ignoreContentType(true)
                .userAgent(userAgent)
                .followRedirects(false)
                .execute().body()
                .letIf({ it.toLowerCase().contains("<html>") }) {
                    Log.w(LOG_TAG, "fetchVersionName()",
                            IOException("Invalid page loaded: $it"))
                    return@letIf null
                }
                ?.replace("\n", "")
    } catch (e: Exception) {
        Log.w(LOG_TAG, "fetchVersionName()", e); null
    }.also {
        Log.d(LOG_TAG, "fetchVersionName() -> (versionName=$it)")
    }

    /*fun fetchChangelog(): String? = try { // TODO: use html page as changelog in new api
        Jsoup.connect(URL_CHANGELOG)
                .followRedirects(false)
                .execute().body()
                .letIf({ it.toLowerCase().contains("<html>") }) {
                    Log.w(LOG_TAG, "fetchChangelog()",
                            IOException("Invalid page loaded: $it"))
                    return@letIf null
                }
                ?.trim()
    } catch (e: Exception) {
        Log.w(LOG_TAG, "fetchChangelog()", e); null
    }.also {
        Log.d(LOG_TAG, "fetchChangelog() -> (changeLog=$it)")
    }*/

    fun showChangelog(context: Context) =
            AndroidUtils.openUri(context, URL_CHANGELOG, R.string.toast_browser_failed)

    fun fetchApk(context: Context): String? {
        // TODO: implement
        return null
    }

    /*@Throws(IOException::class, InterruptedException::class)
    fun downloadUpdate(context: Context, reporter: ProgressReporter, filename: String): String {
        var fos: FileOutputStream? = null
        var `is`: InputStream? = null
        try {
            val url = URL(URL_APK)
            val c = url.openConnection() as HttpURLConnection
            c.requestMethod = "GET"
            c.instanceFollowRedirects = false
            c.doOutput = true
            c.connect()

            val file = context.getExternalFilesDir(Environment
                    .DIRECTORY_DOWNLOADS)
            // TODO: 12.11.2015 save apk to app data if sd card is not available

            file.mkdirs()
            val outputFile = File(file, filename)
            if (outputFile.exists()) outputFile.delete()
            fos = FileOutputStream(outputFile)

            `is` = c.inputStream

            val buffer = ByteArray(1024)
            var len: Int
            var completedLen = 0
            val max = c.contentLength
            reporter.setMaxProgress(if (max != -1) max else Integer.MAX_VALUE)
            //Log.d("UpdateConnector", "TotalLen: " + is.available());
            val currentThread = Thread.currentThread()
            while ((len = `is`.read(buffer)) != -1 && !currentThread.isInterrupted) {
                fos.write(buffer, 0, len)
                completedLen += len
                reporter.reportProgress(completedLen)
                //Log.d("UpdateConnector", "CompletedLen: " + completedLen);
            }

            if (Thread.interrupted())
                throw InterruptedException()

            return outputFile.absolutePath
        } finally {
            if (fos != null) fos.close()
            if (`is` != null) `is`.close()
        }

        *//*File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                + File.separator + filename);
        if (file.exists()) file.delete();

        DownloadManager.Request request = new DownloadManager
                .Request(Uri.parse(DEFAULT_URL + LATEST_APK_URL_ADD));
        request.setTitle(context.getString(R.string.downloading_update));
        request.setDescription(context.getString(R.string.please_wait));

        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= 11) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        } else {
            //noinspection deprecation
            request.setShowRunningNotification(false);
        }
        request.setVisibleInDownloadsUi(false);
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename);

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        //manager.query(new DownloadManager.Query().setFilterById())
        return manager.enqueue(request);*//*
    }*/
}
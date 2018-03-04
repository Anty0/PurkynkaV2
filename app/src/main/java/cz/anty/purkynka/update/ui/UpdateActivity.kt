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

package cz.anty.purkynka.update.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.evernote.android.job.Job
import com.google.firebase.analytics.FirebaseAnalytics
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.R
import cz.anty.purkynka.update.data.AvailableVersionInfo
import cz.anty.purkynka.update.load.UpdateFetcher
import cz.anty.purkynka.update.save.UpdateData
import cz.anty.purkynka.update.sync.Updater
import cz.anty.purkynka.utils.FBA_UPDATE_DOWNLOAD
import cz.anty.purkynka.utils.FBA_UPDATE_INSTALL
import eu.codetopic.java.utils.Anchor
import eu.codetopic.java.utils.alsoIf
import eu.codetopic.java.utils.fillToLen
import eu.codetopic.java.utils.ifFalse
import eu.codetopic.utils.*
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.thread.LooperUtils
import eu.codetopic.utils.thread.progress.ProgressBarReporter
import eu.codetopic.utils.thread.progress.ProgressInfo
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.view.asViewVisibility
import eu.codetopic.utils.ui.view.holder.loading.LoadingModularActivity
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.activity_update.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.Job as KJob
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.lang.ref.WeakReference

/**
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class UpdateActivity : LoadingModularActivity(ToolbarModule(), BackButtonModule()) {

    companion object {

        private const val LOG_TAG = "UpdateActivity"

        fun getIntent(context: Context): Intent =
                Intent(context, UpdateActivity::class.java)

        fun start(context: Context) =
                context.startActivity(getIntent(context))
    }

    private val updateDataChangedReceiver = receiver { _, _ -> update() }

    private var firebaseAnalytics: FirebaseAnalytics? = null

    private val versionCurrent: AvailableVersionInfo =
            AvailableVersionInfo(
                    code = BuildConfig.VERSION_CODE,
                    name = BuildConfig.VERSION_NAME
            )
    private var versionAvailable: AvailableVersionInfo =
            AvailableVersionInfo(
                    code = BuildConfig.VERSION_CODE,
                    name = BuildConfig.VERSION_NAME
            )
    private val updateAvailable: Boolean
        get() = versionCurrent.code != versionAvailable.code

    private var isDownloading: Boolean = false
    private var isDownloaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        boxRefreshLayout.setOnRefreshListener {
            refreshWithRefreshLayout()
        }

        butDownloadUpdate.onClick {
            firebaseAnalytics?.logEvent(FBA_UPDATE_DOWNLOAD, null)
            downloadUpdate()
        }
        butInstallUpdate.onClick {
            firebaseAnalytics?.logEvent(FBA_UPDATE_INSTALL, null)
            installUpdate()
        }
        butShowChangelog.onClick { showChangelog() }

        if (savedInstanceState == null) refreshWithLoading()

        updateWithLoading()
    }

    override fun onDestroy() {
        firebaseAnalytics = null

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.activity_update, menu)

        menu.findItem(R.id.action_refresh).icon =
                getIconics(GoogleMaterial.Icon.gmd_refresh).actionBar()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> refreshWithLoading()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onStart() {
        super.onStart()

        LocalBroadcast.registerReceiver(
                receiver = updateDataChangedReceiver,
                filter = intentFilter(UpdateData.getter)
        )

        update()
    }

    override fun onStop() {
        LocalBroadcast.unregisterReceiver(
                receiver = updateDataChangedReceiver
        )

        super.onStop()
    }

    private fun refreshWithLoading() {
        if (isDownloading || isDownloaded) return // Update is being prepared,
        //  refreshing available version here will have no effect.

        val holder = holder
        launch(UI) {
            holder.showLoading()

            fetchUpdate().join()

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }

    private fun refreshWithRefreshLayout() {
        if (isDownloading || isDownloaded) {
            // If update is being prepared, refreshing available version will have no effect.
            boxRefreshLayout.isRefreshing = false
            return
        }

        val boxRefreshLayoutRef = boxRefreshLayout.asReference()
        launch(UI) {
            fetchUpdate().join()

            // wait for ui update
            delay(500)

            boxRefreshLayoutRef().isRefreshing = false
        }
    }

    private fun fetchUpdate(): Deferred<Job.Result> {
        val self = this.asReference()
        return async(UI) {
            bg { Updater.fetchUpdates() }.await()
                    //.also { Updater.suspendNotifyAboutUpdate(self) }
                    .alsoIf({ it == Job.Result.FAILURE }) {
                        self().snackbarUpdateFetchFailed()
                    }
        }
    }

    private fun snackbarUpdateFetchFailed() = longSnackbar(
            boxRefreshLayout,
            R.string.snackbar_updates_fetch_fail,
            R.string.snackbar_action_updates_retry,
            { refreshWithLoading() }
    )

    private fun downloadUpdate(): KJob? {
        if (isDownloading || isDownloaded || !updateAvailable) return null
        isDownloading = true

        val reporter = CustomProgressBarReporter(
                progressDownload,
                txtDownloadProgress
        )

        updateUi()

        val versionInfo = versionAvailable
        val appContext = applicationContext
        val self = this.asReference()
        return launch(UI) {
            delay(500) // Wait few loops to make sure, that content was updated.

            reporter.startShowingProgress()

            val success = bg { UpdateFetcher.fetchApk(appContext, versionInfo, reporter) }.await()

            reporter.stopShowingProgress()

            val valid = success && bg { UpdateFetcher.checkApk(appContext, versionInfo) }.await()

            delay(500) // Wait few loops to make sure, that content was updated.

            if (!success) {
                longSnackbar(
                        view = self().progressDownload,
                        message = R.string.snackbar_updates_download_update_failed
                )
            } else if (!valid) {
                longSnackbar(
                        view = self().progressDownload,
                        message = R.string.snackbar_updates_download_update_invalid
                )
            }

            self().isDownloading = false
            self().update().join()
        }
    }

    private fun installUpdate() {
        if (!isDownloaded || !updateAvailable || isDownloading) return

        val apkUri = UpdateFetcher.getApkUriFor(this, versionAvailable)
        startActivity(
                Intent(Intent.ACTION_INSTALL_PACKAGE)
                        .setData(apkUri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Intent.EXTRA_RETURN_RESULT, false)
                        .also {
                            @Suppress("DEPRECATION")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                                it.putExtra(Intent.EXTRA_ALLOW_REPLACE, true)
                        }

                /*Intent(Intent.ACTION_VIEW)
                        .setDataAndType(apkUri, "application/vnd.android.package-archive")
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)*/
        )
    }

    private fun showChangelog() = startActivity(ChangelogActivity.getIntent(this))

    private fun updateWithLoading(): KJob {
        val holder = holder
        return launch(UI) {
            holder.showLoading()

            update().join()

            delay(500) // Wait few loops to make sure, that content was updated.
            holder.hideLoading()
        }
    }

    private fun update(): KJob {
        val appContext = applicationContext
        val self = this.asReference()
        return launch(UI) {
            val availableVersion = bg { UpdateData.instance.latestVersion }.await()
            self().versionAvailable = availableVersion

            self().isDownloaded = self().updateAvailable && !self().isDownloading
                    && bg { UpdateFetcher.checkApk(appContext, availableVersion) }.await()

            self().updateUi()
        }
    }

    private fun updateUi() {
        arrayOf(boxUpToDate, boxUpdateAvailable, boxUpdateDownloading, boxUpdateDownloaded)
                .forEach { it.visibility = View.GONE }

        when {
            isDownloading -> {
                boxUpdateDownloading.visibility = View.VISIBLE
                txtUpdateDownloading.text = getFormattedText(
                        R.string.updates_text_update_downloading,
                        versionCurrent.name, versionAvailable.name
                )
            }
            isDownloaded -> {
                boxUpdateDownloaded.visibility = View.VISIBLE
                txtUpdateDownloaded.text = getFormattedText(
                        R.string.updates_text_update_downloaded,
                        versionCurrent.name, versionAvailable.name
                )

                val notes = versionAvailable.notes
                boxUpdateDownloadedNotes.visibility = (notes != null).asViewVisibility()
                txtUpdateDownloadedNotes.text = notes?.let {
                    return@let AndroidUtils.fromHtml(it)
                }
            }
            updateAvailable -> {
                boxUpdateAvailable.visibility = View.VISIBLE
                txtUpdateAvailable.text = getFormattedText(
                        R.string.updates_text_update_available,
                        versionCurrent.name, versionAvailable.name
                )
            }
            else -> {
                boxUpToDate.visibility = View.VISIBLE
                txtUpToDate.text = getFormattedText(
                        R.string.updates_text_up_to_date,
                        versionCurrent.name, versionAvailable.name
                )
            }
        }
    }

    private class CustomProgressBarReporter(
            progressBar: ProgressBar?,
            progressTextView: TextView?
    ) : ProgressBarReporter(progressBar) {

        private val progressTextViewRef = WeakReference(progressTextView)

        override fun onChange(info: ProgressInfo) {
            super.onChange(info)
            LooperUtils.postOnViewThread(progressTextViewRef.get()) {
                progressTextViewRef.get()?.text = SpannableStringBuilder().apply str@ {
                    if (info.isIntermediate) return@str

                    val progressRatio = info.progress.toFloat() / info.maxProgress.toFloat()
                    val progressPercent = (progressRatio * 100F).toInt()

                    append(info.progress.toString())
                    append("/")
                    append(info.maxProgress.toString())
                    append("\n")
                    append(progressPercent.toString())
                    append("%")
                }
            }
        }
    }
}
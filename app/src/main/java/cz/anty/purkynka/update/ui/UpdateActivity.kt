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
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.evernote.android.job.Job
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.R
import cz.anty.purkynka.update.save.UpdateData
import cz.anty.purkynka.update.sync.Updater
import eu.codetopic.java.utils.alsoIf
import eu.codetopic.utils.receiver
import eu.codetopic.utils.getFormattedText
import eu.codetopic.utils.getIconics
import eu.codetopic.utils.intentFilter
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.view.holder.loading.LoadingModularActivity
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.activity_update.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.Job as KJob
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.sdk25.coroutines.onClick

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

    private var versionNameCurrent: String = "unknown"
    private var versionNameAvailable: String = "unknown"
    private var versionCodeCurrent: Int? = null
    private var versionCodeAvailable: Int? = null
    private val updateAvailable: Boolean
        get() = versionCodeCurrent != versionCodeAvailable

    private var isDownloading: Boolean = false // TODO: implement
    private var isDownloaded: Boolean = false // TODO: implement

    private val updateDataChangedReceiver = receiver { _, _ -> update() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)

        boxRefreshLayout.setOnRefreshListener {
            refreshWithRefreshLayout()
        }

        butDownloadUpdate.onClick { downloadUpdate() }
        butInstallUpdate.onClick { installUpdate() }
        butShowChangelog.onClick { showChangelog() }

        updateWithLoading()
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

            fetchUpdate()

            // wait for ui update
            delay(500)

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
            fetchUpdate()

            // wait for ui update
            delay(500)

            boxRefreshLayoutRef().isRefreshing = false
        }
    }

    private suspend fun fetchUpdate(): Job.Result {
        val self = this.asReference()
        return bg { Updater.fetchUpdates() }.await()
                .also { Updater.suspendNotifyAboutUpdate(self) }
                .alsoIf({ it == Job.Result.FAILURE }) {
                    snackbarUpdateFetchFailed()
                }
    }

    private fun snackbarUpdateFetchFailed() = longSnackbar(
            boxRefreshLayout,
            R.string.snackbar_updates_fetch_fail,
            R.string.snackbar_action_updates_retry,
            { refreshWithLoading() }
    )

    private fun downloadUpdate() {
        // TODO: implement
    }

    private fun installUpdate() {
        // TODO: implement

        /*new Intent(Intent.ACTION_VIEW) // Install update intent
            .setDataAndType(Uri.fromFile(new File(path)), "application/vnd.android.package-archive")
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);*/
    }

    private fun showChangelog() = startActivity(ChangelogActivity.getIntent(this))

    private fun updateWithLoading(): KJob {
        val holder = holder
        return launch(UI) {
            holder.showLoading()

            update().join()

            holder.hideLoading()
        }
    }

    private fun update(): KJob {
        val self = this.asReference()
        return launch(UI) {
            self().versionCodeCurrent = BuildConfig.VERSION_CODE
            self().versionNameCurrent = BuildConfig.VERSION_NAME

            self().versionCodeAvailable = bg { UpdateData.instance.latestVersionCode }.await()
            self().versionNameAvailable = bg { UpdateData.instance.latestVersionName }.await()

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
                        versionNameCurrent, versionNameAvailable
                )
            }
            isDownloaded -> {
                boxUpdateDownloaded.visibility = View.VISIBLE
                txtUpdateDownloaded.text = getFormattedText(
                        R.string.updates_text_update_downloaded,
                        versionNameCurrent, versionNameAvailable
                )
            }
            updateAvailable -> {
                boxUpdateAvailable.visibility = View.VISIBLE
                txtUpdateAvailable.text = getFormattedText(
                        R.string.updates_text_update_available,
                        versionNameCurrent, versionNameAvailable
                )
            }
            else -> {
                boxUpToDate.visibility = View.VISIBLE
                txtUpToDate.text = getFormattedText(
                        R.string.updates_text_up_to_date,
                        versionNameCurrent, versionNameAvailable
                )
            }
        }
    }
}
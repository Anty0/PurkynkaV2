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

package cz.anty.purkynka.update

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import cz.anty.purkynka.BuildConfig
import cz.anty.purkynka.R
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.AndroidExtensions.getFormattedText
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.ui.activity.modular.ModularActivity
import eu.codetopic.utils.ui.activity.modular.module.BackButtonModule
import eu.codetopic.utils.ui.activity.modular.module.ToolbarModule
import eu.codetopic.utils.ui.view.holder.loading.LoadingModularActivity
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.activity_update.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg

/**
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class UpdateActivity : LoadingModularActivity(ToolbarModule(), BackButtonModule()) {

    private var versionNameCurrent: String = "unknown"
    private var versionNameAvailable: String = "unknown"
    private var versionCodeCurrent: Int? = null
    private var versionCodeAvailable: Int? = null
    private val updateAvailable: Boolean
        get() = versionCodeCurrent != versionCodeAvailable
    private val isDownloading: Boolean = false // TODO: implement
    private val isDownloaded: Boolean = false // TODO: implement

    private val updateDataChangedReceiver = broadcast { _, _ -> updateData() }

    companion object {

        fun generateIntent(context: Context): Intent =
                Intent(context, UpdateActivity::class.java)

        fun start(context: Context) =
                context.startActivity(generateIntent(context))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)

        butDownloadUpdate.setOnClickListener { downloadUpdate() }
        butInstallUpdate.setOnClickListener { installUpdate() }
        butShowChangelog.setOnClickListener { showChangelog() }

        val holder = holder
        launch(UI) {
            holder.showLoading()

            updateData().join()

            holder.hideLoading()
        }
    }

    override fun onStart() {
        super.onStart()

        LocalBroadcast.registerReceiver(
                receiver = updateDataChangedReceiver,
                filter = intentFilter(UpdateData.getter)
        )

        updateData()
    }

    override fun onStop() {
        LocalBroadcast.unregisterReceiver(
                receiver = updateDataChangedReceiver
        )

        super.onStop()
    }

    private fun downloadUpdate() {
        // TODO: implement
    }

    private fun installUpdate() {
        // TODO: implement

        /*new Intent(Intent.ACTION_VIEW) // Install update intent
            .setDataAndType(Uri.fromFile(new File(path)), "application/vnd.android.package-archive")
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);*/
    }

    private fun showChangelog() {
        // TODO: implement
    }

    private fun updateData() = launch(UI) {
        versionCodeCurrent = BuildConfig.VERSION_CODE
        versionNameCurrent = BuildConfig.VERSION_NAME

        versionCodeAvailable = bg { UpdateData.instance.latestVersionCode }.await()
        versionNameAvailable = bg { UpdateData.instance.latestVersionName }.await()

        updateViews()
    }

    private fun updateViews() {
        arrayOf(boxUpToDate, boxUpdateAvailable, boxUpdateDownloading, boxUpdateDownloaded)
                .forEach { it.visibility = View.GONE }

        when {
            isDownloading -> {

            }
            isDownloaded -> {

            }
            updateAvailable -> {

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
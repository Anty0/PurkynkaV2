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

package cz.anty.purkynka.lunches

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.DatePicker
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.lunches.data.LunchOption
import cz.anty.purkynka.lunches.data.LunchOptionsGroup
import cz.anty.purkynka.lunches.load.LunchesParser
import cz.anty.purkynka.lunches.save.LunchesData
import cz.anty.purkynka.lunches.save.LunchesLoginData
import cz.anty.purkynka.lunches.sync.LunchesBurzaWatcherService
import eu.codetopic.java.utils.JavaExtensions.to
import eu.codetopic.java.utils.JavaExtensions.letIf
import eu.codetopic.java.utils.log.Log
import eu.codetopic.utils.AndroidExtensions.getFormattedText
import eu.codetopic.utils.AndroidExtensions.broadcast
import eu.codetopic.utils.AndroidExtensions.intentFilter
import eu.codetopic.utils.AndroidExtensions.getKSerializableExtra
import eu.codetopic.utils.broadcast.LocalBroadcast
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.fragment_lunches_burza_watcher.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.checkBox
import org.jetbrains.anko.childrenSequence
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.coroutines.experimental.bg
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.support.v4.ctx
import proguard.annotation.KeepName
import java.util.*
import kotlin.math.max

/**
 * @author anty
 */
@KeepName
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class LunchesBurzaWatcherFragment : NavigationFragment(), TitleProvider, ThemeProvider {

    companion object {

        private const val LOG_TAG = "LunchesBurzaWatcherFragment"
    }

    override val title: CharSequence
        get() = getText(R.string.title_fragment_lunches_burza_watcher)
    override val themeId: Int
        get() = R.style.AppTheme_Lunches

    private val accountHolder = ActiveAccountHolder(holder)

    private val loginDataChangedReceiver = broadcast { _, _ ->
        Log.d(LOG_TAG, "loginDataChangedReceiver.onReceive()")
        updateWithLoading()
    }
    private val dataChangedReceiver = broadcast { _, _ ->
        Log.d(LOG_TAG, "dataChangedReceiver.onReceive()")
        update()
    }
    private val watcherServiceStatusChangedReceiver = broadcast { _, intent ->
        Log.d(LOG_TAG, "dataChangedReceiver.onReceive()")

        if (serviceLoading) {
            serviceLoading = false
            holder.hideLoading()
        }

        serviceStatus = intent?.getKSerializableExtra(
                name = LunchesBurzaWatcherService.EXTRA_STATUS_MAP,
                loader = LunchesBurzaWatcherService.EXTRA_STATUS_MAP_SERIALIZER
        ) ?: return@broadcast

        update()
    }

    private var userLoggedIn: Boolean = false
    private var lunchesList: List<LunchOptionsGroup>? = null
    private var isDataValid: Boolean = true
    private var serviceStatus: Map<String, LunchesBurzaWatcherService.BurzaWatcherStatus>? = null
    private var lastAccountServiceStatus: Pair<String, LunchesBurzaWatcherService.BurzaWatcherStatus?>? = null
    private var serviceLoading: Boolean = true

    init {
        val self = this.asReference()
        accountHolder.addChangeListener {
            self().update().join()
            if (!self().userLoggedIn) {
                // App was switched to not logged in user
                // Let's switch fragment
                self().switchFragment(LunchesLoginFragment::class.java)
            }
        }
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
                                     savedInstanceState: Bundle?): View? {
        val themedContext = ContextThemeWrapper(inflater.context, themeId)
        val themedInflater = inflater.cloneInContext(themedContext)
        return themedInflater.inflate(R.layout.fragment_lunches_burza_watcher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Will be hidden when service response for first time
        holder.showLoading()

        inPickDate.apply {
            val calendar = Calendar.getInstance()
            init(
                    calendar[Calendar.YEAR],
                    calendar[Calendar.MONTH],
                    calendar[Calendar.DAY_OF_MONTH]
            ) { _, _, _, _ -> updateUi() }
        }

        butStartWatcher.onClick {
            val accountId = accountHolder.accountId ?: return@onClick
            ContextCompat.startForegroundService(
                    ctx,
                    LunchesBurzaWatcherService.getStartWatcherIntent(
                            context = ctx,
                            accountId = accountId,
                            burzaWatcherArguments = LunchesBurzaWatcherService.BurzaWatcherArguments(
                                    targetDate = inPickDate.getCalendar().timeInMillis,
                                    targetLunchNumbers = run {
                                        boxLunchNumbers.childrenSequence()
                                                .mapNotNull {
                                                    it.to<CheckBox>()
                                                            ?.takeIf { it.isEnabled }
                                                            ?.isChecked
                                                }
                                                .let {
                                                    mutableListOf<Int>().apply {
                                                        it.forEachIndexed { index, checked ->
                                                            if (checked) add(index + 1)
                                                        }
                                                    }.toTypedArray()
                                                }
                                    }
                            )
                    )
            )
        }

        butStopWatcher.onClick {
            val accountId = accountHolder.accountId ?: return@onClick
            ctx.startService(
                    LunchesBurzaWatcherService.getStopWatcherIntent(
                            context = ctx,
                            accountId = accountId
                    )
            )
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        val self = this.asReference()
        val holder = holder
        launch(UI) {
            holder.showLoading()

            arrayOf(
                    self().update(),
                    self().accountHolder.update()
            ).forEach { it.join() }

            holder.hideLoading()
        }
    }

    override fun onStart() {
        super.onStart()

        register()
        accountHolder.register()
    }

    override fun onStop() {
        accountHolder.unregister()
        unregister()

        super.onStop()
    }

    private fun DatePicker.getCalendar(): Calendar =
            Calendar.getInstance().also {
                //it.set(datePicker.year, datePicker.month, datePicker.dayOfMonth, 0, 0, 0)
                it.set(Calendar.YEAR, year)
                it.set(Calendar.MONTH, month)
                it.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                it.set(Calendar.HOUR_OF_DAY, 0)
                it.set(Calendar.MINUTE, 0)
                it.set(Calendar.SECOND, 0)
                it.set(Calendar.MILLISECOND, 0)
            }

    private fun register(): Job {
        LocalBroadcast.registerReceiver(loginDataChangedReceiver,
                intentFilter(LunchesLoginData.getter))
        LocalBroadcast.registerReceiver(dataChangedReceiver,
                intentFilter(LunchesData.getter))
        ctx.registerReceiver(watcherServiceStatusChangedReceiver,
                intentFilter(LunchesBurzaWatcherService.ACTION_STATUS_UPDATE))

        ctx.startService(
                LunchesBurzaWatcherService.getRequestStatusUpdateIntent(ctx)
        )

        return update()
    }

    private fun unregister() {
        ctx.unregisterReceiver(watcherServiceStatusChangedReceiver)
        LocalBroadcast.unregisterReceiver(dataChangedReceiver)
        LocalBroadcast.unregisterReceiver(loginDataChangedReceiver)
    }

    private fun updateWithLoading(): Job {
        val self = this.asReference()
        val holder = holder
        return launch(UI) {
            holder.showLoading()

            self().update().join()

            holder.hideLoading()
        }
    }

    private fun updateLastStatus() {
        val accountId = accountHolder.accountId

        val oldStatus = lastAccountServiceStatus
        val newStatus = accountId?.let {
            accountId to serviceStatus?.get(accountId)
        }

        lastAccountServiceStatus = when {
            oldStatus == null -> newStatus
            newStatus == null ->
                if (oldStatus.first != accountId)
                    null
                else oldStatus
            oldStatus.first != newStatus.first -> newStatus
            newStatus.second?.running == true -> newStatus
            newStatus.second?.arguments != null -> newStatus
            oldStatus.second == null && newStatus.second != null -> newStatus
            else -> oldStatus
        }
    }

    private fun update(): Job {
        val self = this.asReference()
        return launch(UI) {
            self().updateLastStatus()
            self().userLoggedIn = self().accountHolder.accountId?.let {
                bg { LunchesLoginData.loginData.isLoggedIn(it) }.await()
            } ?: false
            self().isDataValid = self().accountHolder.accountId?.let {
                bg { LunchesData.instance.isDataValid(it) }.await()
            } ?: true
            self().lunchesList = self().accountHolder.accountId?.let {
                bg { LunchesData.instance.getLunches(it) }.await()
            }

            self().updateUi()
        }
    }

    private fun updateUi() {
        view ?: return

        // Layout hierarchy:

        // boxStartWatcher
        /// inPickDate
        /// boxLunchNumbers
        // boxWarning
        // txtWarningOrdered
        // txtWarningNoOptions
        /// butStartWatcher

        // boxStopWatcher
        /// butStopWatcher

        // boxStatusLineRunning
        // boxStatusLineStopping
        // boxStatusLineReady
        // boxStatusLineNotReady

        // boxStatusInfo
        //
        /// txtRefreshCount
        //
        /// txtOrderAttemptsCount
        //
        /// txtTargetDate
        //
        /// txtTargetLunch
        //
        // txtSuccess
        // txtNotSuccess
        // txtUnknownSuccess
        //
        // txtNoFail
        // txtFail

        val lastStatusPair = lastAccountServiceStatus
        val lastStatus = lastStatusPair?.second

        val preparing = lastStatusPair == null

        val running = lastStatus?.running == true
        val stopping = lastStatus?.stopping == true
        val success = lastStatus?.success == true
        val fail = lastStatus?.fail == true
        val refreshCount = lastStatus?.refreshCount ?: 0
        val orderCount = lastStatus?.orderCount ?: 0

        val arguments = lastStatus?.arguments
        val targetDate = arguments?.targetDate
        val targetLunchNumbers = arguments?.targetLunchNumbers

        val timeMilis = inPickDate.getCalendar().timeInMillis
        val lunchOptionsGroup = lunchesList
                ?.takeIf { isDataValid }
                ?.firstOrNull { it.date == timeMilis }
        val options = lunchOptionsGroup?.options
        val hasOptions = options?.isEmpty() == false
        val hasOrderedLunch = lunchOptionsGroup?.orderedOption != null

        boxStartWatcher.visibility = if (!preparing && !running) View.VISIBLE else View.GONE

        boxLunchNumbers.apply {
            val oldChecked = childrenSequence()
                    .mapNotNull { it.to<CheckBox>()?.isChecked }
                    .toList()
            removeAllViews()

            val minSize = max(3, oldChecked.count())
            val checkOptions = options
                    ?.letIf({ it.size < minSize }) {
                        arrayOf<LunchOption?>(*it, *arrayOfNulls(minSize - it.size))
                    }
                    ?: arrayOfNulls<LunchOption>(minSize)

            checkOptions.forEachIndexed { index, lunchOption ->
                checkBox {
                    text = SpannableStringBuilder().apply {
                        append(
                                ctx.getFormattedText(
                                        R.string.text_view_lunches_lunch_number,
                                        index + 1
                                )
                        )
                        if (lunchOption != null) {
                            append(" - ")
                            append(lunchOption.name)
                        }
                    }
                    isChecked = oldChecked.getOrElse(index) { true }
                    visibility = if (options == null || lunchOption != null) View.VISIBLE else View.GONE
                }
            }
        }

        boxWarning.visibility = if (!hasOptions || hasOrderedLunch) View.VISIBLE else View.GONE
        txtWarningOrdered.visibility = if (hasOrderedLunch) View.VISIBLE else View.GONE
        txtWarningNoOptions.visibility = if (!hasOptions) View.VISIBLE else View.GONE

        boxStopWatcher.visibility = if (!preparing && running) View.VISIBLE else View.GONE
        butStopWatcher.isEnabled = !stopping

        boxStatusLineRunning.visibility = if (!preparing && running && !stopping) View.VISIBLE else View.GONE
        boxStatusLineStopping.visibility = if (!preparing && running && stopping) View.VISIBLE else View.GONE
        boxStatusLineReady.visibility = if (!preparing && !running) View.VISIBLE else View.GONE
        boxStatusLineNotReady.visibility = if (preparing) View.VISIBLE else View.GONE

        boxStatusInfo.visibility = if (arguments != null) View.VISIBLE else View.GONE

        txtRefreshCount.text = refreshCount.toString()

        txtOrderAttemptsCount.text = orderCount.toString()

        txtTargetDate.text = targetDate
                ?.let { LunchesParser.FORMAT_DATE_SHOW.format(it) }
                ?: ctx.getText(R.string.text_view_lunches_lunch_date_unknown)

        txtTargetLunch.text = SpannableStringBuilder().apply {
            if (targetLunchNumbers != null) {
                targetLunchNumbers
                        .map {
                            ctx.getFormattedText(
                                    R.string.text_view_lunches_lunch_number,
                                    it
                            )
                        }
                        .forEachIndexed { i, it ->
                            if (i != 0) append("\n")
                            append(it)
                        }
            } else append(ctx.getText(R.string.text_view_lunches_burza_lunch_number_unknown))

        }

        txtSuccess.visibility = if (success) View.VISIBLE else View.GONE
        txtNotSuccess.visibility = if (!success && !running) View.VISIBLE else View.GONE
        txtUnknownSuccess.visibility = if (!success && running) View.VISIBLE else View.GONE

        txtNoFail.visibility = if (!fail) View.VISIBLE else View.GONE
        txtFail.visibility = if (fail) View.VISIBLE else View.GONE
    }
}
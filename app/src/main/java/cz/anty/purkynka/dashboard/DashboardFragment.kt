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

package cz.anty.purkynka.dashboard

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import cz.anty.purkynka.R
import cz.anty.purkynka.account.ActiveAccountHolder
import cz.anty.purkynka.grades.dashboard.NewGradesDashboardManager
import cz.anty.purkynka.grades.dashboard.SubjectsAverageDashboardManager
import cz.anty.purkynka.update.dashboard.UpdateCheckDashboardManager
import cz.anty.purkynka.utils.ICON_HOME_DASHBOARD
import eu.codetopic.utils.getIconics
import eu.codetopic.java.utils.to
import eu.codetopic.java.utils.letIfNull
import eu.codetopic.utils.ui.activity.fragment.IconProvider
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import eu.codetopic.utils.ui.container.adapter.MultiAdapter
import eu.codetopic.utils.ui.container.adapter.UniversalAdapter
import eu.codetopic.utils.ui.container.adapter.UniversalAdapter.RecyclerBase.UniversalViewHolder
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.recycler.Recycler
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.support.v4.ctx

/**
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class DashboardFragment : NavigationFragment(), TitleProvider, ThemeProvider, IconProvider {

    companion object {

        private const val LOG_TAG = "DashboardFragment"
    }

    override val title: CharSequence
        get() = getText(R.string.title_fragment_dashboard)
    override val themeId: Int
        get() = R.style.AppTheme
    override val icon: Bitmap
        get() = ctx.getIconics(ICON_HOME_DASHBOARD).sizeDp(48).toBitmap()

    private val accountHolder = ActiveAccountHolder(holder)

    private var recyclerManager: Recycler.RecyclerManagerImpl? = null
    private var managers: List<DashboardManager>? = null
    private var adapter: MultiAdapter<DashboardItem>? = null

    init {
        val self = this.asReference()
        accountHolder.addChangeListener {
            self().managers
                    ?.map { it.update() }
                    ?.forEach { it?.join() }
        }
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
                                     savedInstanceState: Bundle?): View? {
        val themedContext = ContextThemeWrapper(inflater.context, themeId)
        val themedInflater = inflater.cloneInContext(themedContext)

        val adapter = MultiAdapter<DashboardItem>(
                context = themedContext,
                comparator = Comparator { o1, o2 -> o2.priority - o1.priority }
        )
        this.adapter = adapter

        managers = listOf(
                UpdateCheckDashboardManager(themedContext, accountHolder, adapter),
                SubjectsAverageDashboardManager(themedContext, accountHolder, adapter),
                NewGradesDashboardManager(themedContext, accountHolder, adapter)
                // TODO: add here all items managers
        )

        val manager = Recycler.inflate()
                .on(themedInflater, container, false)
                .setEmptyImage(themedContext.getIconics(ICON_HOME_DASHBOARD).sizeDp(72))
                .setEmptyText(R.string.empty_view_text_dashboard)
                .setSmallEmptyText(R.string.empty_view_text_small_dashboard)
                .setAdapter(adapter)
                .setItemTouchHelper(object : ItemTouchHelper.Callback() {

                    fun getItem(viewHolder: RecyclerView.ViewHolder): SwipeableDashboardItem? =
                            viewHolder.adapterPosition.takeIf { it != -1 }
                                    ?.let { adapter.getItem(it) }
                                    .to<SwipeableDashboardItem>()

                    fun getItemHolder(viewHolder: RecyclerView.ViewHolder) =
                            viewHolder.to<UniversalViewHolder<UniversalAdapter.ViewHolder>>()
                                    ?.universalHolder
                                    ?.let { CustomItem.ViewHolder.fromUniversalHolder(it) }

                    override fun getMovementFlags(recyclerView: RecyclerView,
                                                  viewHolder: RecyclerView.ViewHolder): Int {
                        val flags = run flags@ {
                            val item = getItem(viewHolder) ?: return@flags 0
                            val holder = getItemHolder(viewHolder) ?: return@flags 0

                            return@flags item.getSwipeDirections(holder)
                        }

                        return makeMovementFlags(0, flags)
                    }

                    override fun onMove(recyclerView: RecyclerView,
                                        viewHolder: RecyclerView.ViewHolder,
                                        target: RecyclerView.ViewHolder): Boolean {
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val item = getItem(viewHolder) ?: return
                        val holder = getItemHolder(viewHolder) ?: return

                        item.onSwiped(holder, direction)
                    }
                })
        recyclerManager = manager
        return manager.baseView
    }

    override fun onDestroyView() {
        recyclerManager = null
        managers = null
        adapter = null

        super.onDestroyView()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        updateWithLoading()
    }

    override fun onStart() {
        super.onStart()

        register()
    }

    override fun onStop() {
        unregister()

        super.onStop()
    }

    private fun register() {
        managers?.forEach { it.register() }
        accountHolder.register()
    }

    private fun unregister() {
        accountHolder.unregister()
        managers?.forEach { it.unregister() }
    }

    private fun update(): Job? {
        val jobs = managers
                ?.map { it.update() }
                .letIfNull { emptyList() }
                .plus(accountHolder.update())

        return launch(UI) {
            jobs.forEach { it?.join() }
        }
    }

    private fun updateWithLoading(): Job? {
        val holder = holder
        val self = this.asReference()
        return launch(UI) {
            holder.showLoading()

            val jobs = self().managers?.map { it.update() }
            self().accountHolder.update().join()
            jobs?.forEach { it?.join() }

            holder.hideLoading()
        }
    }
}
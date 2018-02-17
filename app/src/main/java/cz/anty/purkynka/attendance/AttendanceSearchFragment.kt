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

package cz.anty.purkynka.attendance

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.widget.SearchView
import android.view.*
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import cz.anty.purkynka.utils.Constants.ICON_ATTENDANCE
import cz.anty.purkynka.R
import cz.anty.purkynka.attendance.ui.AttendanceAdapter
import eu.codetopic.java.utils.JavaExtensions.to
import eu.codetopic.utils.AndroidExtensions.getIconics
import eu.codetopic.utils.ui.activity.fragment.IconProvider
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import eu.codetopic.utils.ui.container.recycler.Recycler
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.appcompat.v7.coroutines.onQueryTextListener
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.support.v4.ctx


/**
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class AttendanceSearchFragment : NavigationFragment(), TitleProvider, ThemeProvider, IconProvider {
    // TODO: add search indexing support

    companion object {

        private const val LOG_TAG = "AttendanceSearchFragment"

        private const val SAVE_QUERY = "cz.anty.purkynka.attendance.$LOG_TAG"
    }

    override val title: CharSequence
        get() = getText(R.string.title_fragment_attendance_search)
    override val themeId: Int
        get() = R.style.AppTheme_Attendance
    override val icon: Bitmap
        get() = ctx.getIconics(ICON_ATTENDANCE).sizeDp(48).toBitmap()

    private var recyclerManager: Recycler.RecyclerManagerImpl? = null
    private var adapter: AttendanceAdapter? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
                                     savedInstanceState: Bundle?): View? {
        val themedContext = ContextThemeWrapper(inflater.context, themeId)
        val themedInflater = inflater.cloneInContext(themedContext)

        val query = savedInstanceState?.getString(SAVE_QUERY) ?: ""

        adapter = AttendanceAdapter(themedContext).also {
            val adapterRef = it.asReference()
            launch(UI) {
                holder.showLoading()

                adapterRef().setQuery(query)?.join()

                holder.hideLoading()
            }
        }

        val manager = Recycler.inflate().withSwipeToRefresh().withItemDivider()
                .on(themedInflater, container, false)
                .setEmptyImage(themedContext.getIconics(ICON_ATTENDANCE).sizeDp(72))
                .setEmptyText(R.string.empty_view_text_attendance_people)
                .setSmallEmptyText(R.string.empty_view_text_small_attendance_people)
                .setAdapter(adapter)
                .setOnRefreshListener refresh@ { ->
                    val adapterRef = adapter?.asReference() ?: return@refresh
                    val recyclerManagerRef = recyclerManager?.asReference() ?: return@refresh
                    launch(UI) {
                        adapterRef().reset()?.join()
                        recyclerManagerRef().setRefreshing(false)
                    }
                }
        recyclerManager = manager
        return manager.baseView
    }

    override fun onDestroyView() {
        recyclerManager = null
        adapter = null

        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(SAVE_QUERY, adapter?.query ?: "")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.fragment_attendance_search, menu)

        menu.findItem(R.id.action_search).apply {
            icon = activity
                    ?.getIconics(GoogleMaterial.Icon.gmd_search)
                    ?.actionBar()
            actionView.to<SearchView>()
                    ?.apply {
                        setIconifiedByDefault(true)
                        onQueryTextListener {
                            val holder = holder
                            val self = this@AttendanceSearchFragment.asReference()
                            onQueryTextSubmit(returnValue = true) {
                                holder.showLoading()

                                self().adapter?.setQuery(it ?: "")?.join()

                                holder.hideLoading()
                                return@onQueryTextSubmit true
                            }
                        }
                    }
        }

        menu.findItem(R.id.action_refresh).icon = activity
                ?.getIconics(GoogleMaterial.Icon.gmd_refresh)
                ?.actionBar()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            /*R.id.action_search -> { // not working :(
                item.actionView.to<SearchView>()
                        ?.setQuery(
                                adapter?.query ?: "",
                                false
                        )
            }*/
            R.id.action_refresh -> {
                run refresh@ {
                    val holder = holder
                    val adapterRef = adapter?.asReference() ?: return@refresh
                    launch(UI) {
                        holder.showLoading()
                        adapterRef().reset()?.join()
                        holder.hideLoading()
                    }
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
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

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import cz.anty.purkynka.utils.Constants.ICON_ATTENDANCE
import cz.anty.purkynka.R
import eu.codetopic.utils.AndroidExtensions.getIconics
import eu.codetopic.utils.ui.activity.fragment.IconProvider
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider
import eu.codetopic.utils.ui.activity.fragment.TitleProvider
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment
import eu.codetopic.utils.ui.container.adapter.CustomItemAdapter
import eu.codetopic.utils.ui.container.items.custom.CustomItem
import eu.codetopic.utils.ui.container.recycler.Recycler
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import org.jetbrains.anko.support.v4.ctx

/**
 * @author anty
 */
@ContainerOptions(CacheImplementation.SPARSE_ARRAY)
class AttendanceSearchFragment : NavigationFragment(), TitleProvider, ThemeProvider, IconProvider {
    // TODO: finish implementation

    override val title: CharSequence
        get() = getText(R.string.title_fragment_attendance_search)
    override val themeId: Int
        get() = R.style.AppTheme_Attendance
    override val icon: Bitmap
        get() = ctx.getIconics(ICON_ATTENDANCE).sizeDp(48).toBitmap()

    private var recyclerManager: Recycler.RecyclerManagerImpl? = null
    private var adapter: CustomItemAdapter<CustomItem>? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        adapter = CustomItemAdapter(context)
    }

    override fun onDetach() {
        adapter = null

        super.onDetach()
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?,
                                     savedInstanceState: Bundle?): View? {
        val themedContext = ContextThemeWrapper(inflater.context, themeId)
        val themedInflater = inflater.cloneInContext(themedContext)

        val manager = Recycler.inflate().withSwipeToRefresh().withItemDivider()
                .on(themedInflater, container, false)
                .setEmptyImage(themedContext.getIconics(ICON_ATTENDANCE).sizeDp(72))
                .setEmptyText(R.string.empty_view_text_attendance_people)
                .setSmallEmptyText(R.string.empty_view_text_small_attendance_people)
                .setAdapter(adapter)
                //.setOnRefreshListener { -> requestSyncWithRecyclerRefreshing() }
        recyclerManager = manager
        return manager.baseView
    }

    override fun onDestroyView() {
        recyclerManager = null

        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return super.onOptionsItemSelected(item)
    }
}
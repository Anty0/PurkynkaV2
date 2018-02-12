/*
 * app
 * Copyright (C)   2017  anty
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

package cz.anty.purkynka.dashboard;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import cz.anty.purkynka.Constants;
import cz.anty.purkynka.R;
import eu.codetopic.utils.AndroidExtensions;
import eu.codetopic.utils.AndroidUtils;
import eu.codetopic.utils.ui.activity.fragment.TitleProvider;
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider;
import eu.codetopic.utils.ui.container.adapter.dashboard.DashboardFragmentBase;

/**
 * Created by anty on 6/16/17.
 *
 * @author anty
 */
public class DashboardFragment extends DashboardFragmentBase implements TitleProvider, ThemeProvider {

    public DashboardFragment() {
        super(); // TODO: 6/16/17 create getters and add them here
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getRecyclerManager()
                .setEmptyImage(AndroidExtensions.INSTANCE.getIconics(
                        view.getContext(),
                        Constants.INSTANCE.getICON_HOME_DASHBOARD()
                ).sizeDp(72))
                .setEmptyText(R.string.empty_view_text_dashboard)
                .setSmallEmptyText(R.string.empty_view_text_small_dashboard);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater); // TODO: 2.6.16 add options to menu
    }

    @NonNull
    @Override
    public CharSequence getTitle() {
        return getText(R.string.action_show_dashboard);
    }

    @Override
    public int getThemeId() {
        return R.style.AppTheme;
    }
}

/*
 * Copyright 2017 Jiří Kuchyňka (Anty)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package cz.anty.purkynka.dashboard;

import android.view.Menu;
import android.view.MenuInflater;

import cz.anty.purkynka.R;
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);// TODO: 2.6.16 add options to menu
    }

    @Override
    public CharSequence getTitle() {
        return getText(R.string.action_show_dashboard);
    }

    @Override
    public int getThemeId() {
        return R.style.AppTheme;
    }
}

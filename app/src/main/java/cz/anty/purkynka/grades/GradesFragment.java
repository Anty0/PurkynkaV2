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

package cz.anty.purkynka.grades;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import cz.anty.purkynka.R;
import cz.anty.purkynka.accounts.ActiveAccountManager;
import cz.anty.purkynka.grades.data.Grade;
import cz.anty.purkynka.grades.save.GradesData;
import eu.codetopic.utils.ui.activity.fragment.TitleProvider;
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider;
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment;
import eu.codetopic.utils.ui.container.adapter.CustomItemAdapter;
import eu.codetopic.utils.ui.container.recycler.Recycler;
import eu.codetopic.utils.ui.container.recycler.utils.RecyclerItemClickListener.SimpleClickListener;

/**
 * @author anty
 */
public class GradesFragment extends NavigationFragment implements TitleProvider, ThemeProvider {

    @BindView(R.id.container_recycler)
    public FrameLayout mRecyclerContainer;
    @BindView(R.id.container_login)
    public ScrollView mLoginContainer;

    @BindView(R.id.but_login)
    public Button mLoginButton;
    @BindView(R.id.edit_username)
    public EditText mInputUsername;
    @BindView(R.id.edit_password)
    public EditText mInputPassword;

    private Unbinder mUnbinder = null;

    private Recycler.RecyclerManagerImpl mRecyclerManager = null;
    private CustomItemAdapter<Grade> mAdapter = null;

    private final BroadcastReceiver mGradesLoginDataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateViews();
        }
    };

    public GradesFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new CustomItemAdapter<>(getContext());
        getContext().registerReceiver(mGradesLoginDataChangedReceiver,
                new IntentFilter(GradesData.Companion.getGetter().getDataChangedBroadcastAction()));
    }

    @Nullable
    @Override
    public View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LayoutInflater localInflater = inflater.cloneInContext(
                new ContextThemeWrapper(inflater.getContext(), R.style.AppTheme_Grades));
        View baseView = localInflater.inflate(R.layout.fragment_grades, container, false);
        mUnbinder = ButterKnife.bind(this, baseView);

        mLoginButton.setOnClickListener(view -> {
            GradesData.Companion.getInstance().getLoginData().login(
                    ActiveAccountManager.Companion.getGetter().get().getActiveAccountId(),
                    mInputUsername.getText().toString(),
                    mInputPassword.getText().toString());
        }); // TODO: implement right way

        mRecyclerManager = Recycler.inflate().withSwipeToRefresh()
                .on(localInflater, mRecyclerContainer, true)
                //.setEmptyImage() // TODO: add
                .setEmptyText("No grades") // TODO: to strings
                .setAdapter(mAdapter)
                .setOnRefreshListener(view -> {}) // TODO: implement
                .setItemTouchListener(new SimpleClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        super.onClick(view, position); // TODO: implement
                    }
                });

        updateViews();
        return baseView;
    }

    public void updateViews() {
        if (mUnbinder == null) return;
        if (GradesData.Companion.getGetter().get().getLoginData().isLoggedIn(
                ActiveAccountManager.Companion.getGetter().get().getActiveAccountId())) {
            mRecyclerContainer.setVisibility(View.VISIBLE);
            mLoginContainer.setVisibility(View.GONE);
        } else {
            mRecyclerContainer.setVisibility(View.GONE);
            mLoginContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        mRecyclerManager = null;
        mUnbinder.unbind();
        mUnbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mAdapter = null;
        super.onDestroy();
    }

    @Override
    public CharSequence getTitle() {
        return getText(R.string.action_show_grades);
    }

    @Override
    public int getThemeId() {
        return R.style.AppTheme_Grades;
    }
}

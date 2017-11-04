package cz.anty.purkynka.marks;

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
import cz.anty.purkynka.marks.data.Mark;
import eu.codetopic.utils.AndroidUtils;
import eu.codetopic.utils.ui.activity.fragment.TitleProvider;
import eu.codetopic.utils.ui.activity.fragment.ThemeProvider;
import eu.codetopic.utils.ui.activity.navigation.NavigationFragment;
import eu.codetopic.utils.ui.container.adapter.CustomItemAdapter;
import eu.codetopic.utils.ui.container.recycler.Recycler;
import eu.codetopic.utils.ui.container.recycler.utils.RecyclerItemClickListener.SimpleClickListener;

/**
 * Created by anty on 6/20/17.
 *
 * @author anty
 */
public class MarksFragment extends NavigationFragment implements TitleProvider, ThemeProvider {

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
    private CustomItemAdapter<Mark> mAdapter = null;

    private final BroadcastReceiver mMarksLoginDataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateViews();
        }
    };

    public MarksFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new CustomItemAdapter<>(getContext());
        getContext().registerReceiver(mMarksLoginDataChangedReceiver,
                new IntentFilter(MarksLoginData.getter.getDataChangedBroadcastAction()));
    }

    @Nullable
    @Override
    public View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LayoutInflater localInflater = inflater.cloneInContext(
                new ContextThemeWrapper(inflater.getContext(), R.style.AppTheme_Marks));
        View baseView = localInflater.inflate(R.layout.fragment_marks, container, false);
        mUnbinder = ButterKnife.bind(this, baseView);

        mLoginButton.setOnClickListener(view -> {
            MarksLoginData.getter.get().login(mInputUsername.getText().toString(),
                    mInputPassword.getText().toString());
        }); // TODO: implement right way

        mRecyclerManager = Recycler.inflate().withSwipeToRefresh()
                .on(localInflater, mRecyclerContainer, true)
                //.setEmptyImage() // TODO: add
                .setEmptyText("No marks") // TODO: to strings
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
        if (MarksLoginData.getter.get().isLoggedIn()) {
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
        return getText(R.string.action_show_marks);
    }

    @Override
    public int getThemeId() {
        return R.style.AppTheme_Marks;
    }
}

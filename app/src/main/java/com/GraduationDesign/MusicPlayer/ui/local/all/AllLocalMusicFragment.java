package com.GraduationDesign.MusicPlayer.ui.local.all;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.GraduationDesign.MusicPlayer.R;
import com.GraduationDesign.MusicPlayer.RxBus;
import com.GraduationDesign.MusicPlayer.data.model.PlayList;
import com.GraduationDesign.MusicPlayer.data.model.Song;
import com.GraduationDesign.MusicPlayer.data.source.AppRepository;
import com.GraduationDesign.MusicPlayer.event.PlayListUpdatedEvent;
import com.GraduationDesign.MusicPlayer.event.PlaySongEvent;
import com.GraduationDesign.MusicPlayer.ui.base.BaseFragment;
import com.GraduationDesign.MusicPlayer.ui.base.adapter.OnItemClickListener;
import com.GraduationDesign.MusicPlayer.ui.common.DefaultDividerDecoration;
import com.GraduationDesign.MusicPlayer.ui.playlist.AddToPlayListDialogFragment;
import com.GraduationDesign.MusicPlayer.ui.widget.RecyclerViewFastScroller;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import java.util.List;

/**
 * Created with Android Studio.
 * User: ryan.hoo.j@gmail.com
 * Date: 9/1/16
 * Time: 9:58 PM
 * Desc: LocalFilesFragment
 */
public class AllLocalMusicFragment extends BaseFragment implements LocalMusicContract.View,LocalMusicAdapter.ActionCallback {

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.fast_scroller)
    RecyclerViewFastScroller fastScroller;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.text_view_empty)
    View emptyView;

    LocalMusicAdapter mAdapter;
    LocalMusicContract.Presenter mPresenter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all_local_music, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        mAdapter = new LocalMusicAdapter(getActivity(), null);
        mAdapter.setActionCallback(this);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Song song = mAdapter.getItem(position);
                RxBus.getInstance().post(new PlaySongEvent(song));
            }
        });
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DefaultDividerDecoration());

        fastScroller.setRecyclerView(recyclerView);

        new LocalMusicPresenter(AppRepository.getInstance(), this).subscribe();
    }

    // RxBus Events

    @Override
    protected Subscription subscribeEvents() {
        return RxBus.getInstance().toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        if (o instanceof PlayListUpdatedEvent) {
                            mPresenter.loadLocalMusic();
                        }
                    }
                })
                .subscribe();
    }

    // Adapter Action Callback

    @Override
    public void onAction(View actionView, final int position) {
        final Song song = mAdapter.getItem(position);
        PopupMenu actionMenu = new PopupMenu(getActivity(), actionView, Gravity.END | Gravity.BOTTOM);
        actionMenu.inflate(R.menu.music_action);
        actionMenu.getMenu().findItem(R.id.menu_item_delete).setVisible(false);
        actionMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_item_add_to_play_list:
                        new AddToPlayListDialogFragment()
                                .setCallback(new AddToPlayListDialogFragment.Callback() {
                                    @Override
                                    public void onPlayListSelected(PlayList playList) {
                                        mPresenter.addSongToPlayList(song, playList);
                                    }
                                })
                                .show(getActivity().getSupportFragmentManager().beginTransaction(), "AddToPlayList");
                        break;
//                    case R.id.menu_item_delete:
//                        mDeleteIndex = position;
//                        mPresenter.delete(song, mPlayList);
//                        break;
                }
                return true;
            }
        });
        actionMenu.show();
    }

    // MVP View

    @Override
    public void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void emptyView(boolean visible) {
        emptyView.setVisibility(visible ? View.VISIBLE : View.GONE);
        fastScroller.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    @Override
    public void handleError(Throwable error) {
        Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocalMusicLoaded(List<Song> songs) {
        mAdapter.setData(songs);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void setPresenter(LocalMusicContract.Presenter presenter) {
        mPresenter = presenter;
    }
}

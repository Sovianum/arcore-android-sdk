package com.google.ar.core.examples.java.helloar.quest.quests;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.ar.core.examples.java.helloar.R;
import com.google.ar.core.examples.java.helloar.model.Quest;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class QuestsListFragment extends Fragment {
    public static final String TAG = QuestsListFragment.class.getSimpleName();

    @BindView(R.id.questsRecyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;

    private QuestAdapter adapter;
    private QuestAdapter.OnItemClickListener onItemClickListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.fragment_quests_list, container, false);
        ButterKnife.bind(this, view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

       adapter = new QuestAdapter(this, new ArrayList<Quest>(), onItemClickListener);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshItems();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        setRefreshing(true);
        refreshItems();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void refreshItems() {
        List<Quest> quests = new ArrayList<>();
        Quest quest = new Quest("Название квеста", "Lorem Ipsum - это текст- рыба, часто используемый в печати и вэб-дизайне.\n" +
                "             Lorem Ipsum является стандартной рыбой для текстов на латинице с начала XVI века.\n" +
                "             В то время некий безымянный печатник создал большую коллекцию размеров и форм шрифтов,\n" +
                "             используя Lorem Ipsum для распечатки образцов", 4.5f);
        quests.add(quest);
        quests.add(quest);
        quests.add(quest);
        quests.add(quest);
        quests.add(quest);
        quests.add(quest);

        loadItems(quests);
        setRefreshing(false);
    }

    public void loadItems(List<Quest> quests) {
        if (adapter != null) {
            adapter.serItems(quests);
        }
    }

    public void setRefreshing(boolean refreshing) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    public void setOnItemClickListener(QuestAdapter.OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }



}

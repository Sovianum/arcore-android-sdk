package com.google.ar.core.examples.java.helloar.quest.journal;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.ar.core.examples.java.helloar.App;
import com.google.ar.core.examples.java.helloar.GameModule;
import com.google.ar.core.examples.java.helloar.R;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class JournalFragment extends Fragment {
    public static final String TAG = JournalFragment.class.getSimpleName();

    private JournalMessageAdapter adapter;

    @BindView(R.id.journalRecyclerView)
    RecyclerView recyclerView;

    @Inject
    GameModule gameModule;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        App.getAppComponent().inject(this);
        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.fragment_journal, container, false);
        ButterKnife.bind(this, view);

        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(manager);

        adapter = new JournalMessageAdapter();
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshItems();
    }

    private void refreshItems() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}

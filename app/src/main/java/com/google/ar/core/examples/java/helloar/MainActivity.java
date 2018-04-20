package com.google.ar.core.examples.java.helloar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.ar.core.examples.java.helloar.auth.AuthActivity;
import com.google.ar.core.examples.java.helloar.core.game.InteractionResult;
import com.google.ar.core.examples.java.helloar.core.game.Item;
import com.google.ar.core.examples.java.helloar.core.game.Place;
import com.google.ar.core.examples.java.helloar.core.game.journal.Journal;
import com.google.ar.core.examples.java.helloar.core.game.slot.Slot;
import com.google.ar.core.examples.java.helloar.model.Quest;
import com.google.ar.core.examples.java.helloar.network.Download;
import com.google.ar.core.examples.java.helloar.network.DownloadService;
import com.google.ar.core.examples.java.helloar.network.NetworkModule;
import com.google.ar.core.examples.java.helloar.quest.ARFragment;
import com.google.ar.core.examples.java.helloar.quest.QuestFragment;
import com.google.ar.core.examples.java.helloar.quest.game.ActorPlayer;
import com.google.ar.core.examples.java.helloar.quest.game.QuestModule;
import com.google.ar.core.examples.java.helloar.quest.items.ItemAdapter;
import com.google.ar.core.examples.java.helloar.quest.items.ItemsListFragment;
import com.google.ar.core.examples.java.helloar.quest.journal.JournalFragment;
import com.google.ar.core.examples.java.helloar.quest.place.PlaceFragment;
import com.google.ar.core.examples.java.helloar.quest.quests.QuestsListFragment;
import com.google.ar.core.examples.java.helloar.settings.SettingsFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private LocationListener onLocationChangeListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    @BindView(R.id.bottom_navigation)
    BottomNavigationView bottomNavigationView;

    private QuestsListFragment questsListFragment;
    private ARFragment arFragment;
    private QuestFragment questFragment;
    private ItemsListFragment itemsListFragment;
    private JournalFragment journalFragment;
    private PlaceFragment placeFragment;
    private SettingsFragment settingsFragment;

    @BindView(R.id.toolbar_actionbar)
    Toolbar toolBar;

    @Inject
    GameModule gameModule;

    @Inject
    QuestModule questModule;

    @Inject
    NetworkModule networkModule;

    @Inject
    HintModule hintModule;

    private QuestsListFragment.OnQuestReactor showQuestInfoCallback = new QuestsListFragment.OnQuestReactor() {
        @Override
        public void onQuestReact(Quest quest) {
            goToCurrentQuest();
        }

        @Override
        public void onDowloadReact(Quest quest) {

        }
    };

    private QuestsListFragment.OnQuestReactor startQuestCallback = new QuestsListFragment.OnQuestReactor() {
        @Override
        public void onQuestReact(final Quest quest) {
            final String msg;
            boolean needLoad;
            Quest currQuest = gameModule.getCurrentQuest();

            if (quest == null) {
                msg = "Попытка загрузить null-квест";
                needLoad = false;
            } else if (currQuest == null) {
                needLoad = true;
                msg = "Вы выбрали квест " + quest.getTitle();
            } else {
                needLoad = quest.getId() != currQuest.getId();
                msg = needLoad ? "Вы выбрали квест " + quest.getTitle() : "Вы уже играете в этот квест";
            }
            if (needLoad) {
                Slot currInventory = gameModule.getCurrentInventory();
                if (currInventory != null) {
                    currInventory.clear();
                }
                ActorPlayer player = gameModule.getPlayer();
                if (player != null) {
                    player.release();
                }
                Journal<String> journal = gameModule.getCurrentJournal();
                if (journal != null) {
                    journal.clear();
                }

                gameModule.setCurrentQuest(quest);
                gameModule.getScene().clear();
            }

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            MainActivity.this,
                            msg,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }

        @Override
        public void onDowloadReact(final Quest quest) {
            final String msg;

            if (quest == null) {
                msg = "Попытка загрузить null-квест";
            } else {
                msg = "Вы загружаете квест " + quest.getTitle();
                startDownload(quest.getId());
            }

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            MainActivity.this,
                            msg,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }
    };

    private ItemAdapter.OnItemClickListener chooseItemOnClickListener = new ItemAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(Item item) {
            gameModule.getPlayer().hold(item);
            Toast.makeText(MainActivity.this, "You selected: " + item.getName(), Toast.LENGTH_SHORT).show();
            //TODO action to choose element
        }
    };

    private View.OnClickListener onCloseARClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            onBackPressed();
        }
    };

    private View.OnClickListener onLogoutClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            removeToken();
//            checkAuthorization();
        }
    };

    private AlertDialog alertDialog;

    private BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                    switch (item.getItemId()) {
                        case R.id.action_quests:
                            selectFragment(questsListFragment, QuestsListFragment.TAG, false);
                            break;
                        case R.id.action_current_quest:
                            goToCurrentQuest();
                            break;
                        case R.id.action_ar:
                            goARFragment();
                            break;

                        case R.id.action_settings:
                            selectFragment(settingsFragment, SettingsFragment.TAG, false);
                            break;
                    }
                    return false;
                }
            };

    private boolean showTutorialSuggestion = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getAppComponent().inject(this);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);

        // the order of calls below is important
        try {
            setUpArFragment();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        setUpGameModule();
        setUpQuestFragment();

        questsListFragment = new QuestsListFragment();
        questsListFragment.setQuestCardClickedListener(showQuestInfoCallback);
        questsListFragment.setStartQuestCallback(startQuestCallback);

        setSupportActionBar(toolBar);

        bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        settingsFragment = new SettingsFragment();
        settingsFragment.setOnLogoutClickListener(onLogoutClickListener);

        //startService(new Intent(this, GeolocationService.class));

        //checkAuthorization(); //commented for focus group testing
        selectFragment(questsListFragment, QuestsListFragment.TAG, false);
        registerDownloadReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        showGreeting();
        if (showTutorialSuggestion && PermissionHelper.hasPermissions(this)) {
            showTutorialSuggestion();
            showTutorialSuggestion = false;
        }
        setFirstLaunch(false);
        hintModule.setActivity(this);
        selectFragmentByView(questsListFragment, QuestsListFragment.TAG);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        //System.out.println("Destroy activity");
        //stopService(new Intent(this, GeolocationService.class));
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
            List<String> fragmentsName = new ArrayList<>();
            for (int fragmentIndex = 0; fragmentIndex < backStackEntryCount; fragmentIndex++) {
                fragmentsName.add(getSupportFragmentManager().getBackStackEntryAt(fragmentIndex).getName());
            }
            setBottomNavItemColor(fragmentsName.get(fragmentsName.size() - 2));
            setToolBarByFragment(fragmentsName.get(fragmentsName.size() - 2));
            showOrHideBars(fragmentsName.get(fragmentsName.size() - 2));
            super.onBackPressed();
        } else {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionHelper.hasPermissions(this)) {
            showNoPermission();
        } else {
            if (showTutorialSuggestion) {
                showTutorialSuggestion();
                showTutorialSuggestion = false;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tool_bar, menu);
        setToolBarTitle(getString(R.string.default_fragment_toolbar_title));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                showTutorialSuggestion();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onInteractionResult(InteractionResult interactionResult) {
        if (interactionResult.getType().equals(InteractionResult.Type.QUEST_END)) {
            showCongratulation();
        }
    }

    private void showGreeting() {
        if (isFirstLaunch()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.greeting_message)
                    .setTitle(R.string.greeting_title)
                    .setCancelable(true)
                    .setNeutralButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    checkAndRequestPermissions();
                                }
                            });

            alertDialog = builder.create();
            alertDialog.show();
        } else if (!PermissionHelper.hasPermissions(this)) {
            showNoPermission();
        }
    }

    private void showNoPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.non_permission_message)
                .setTitle(R.string.non_permission_title)
                .setCancelable(false)
                .setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                checkAndRequestPermissions();
                            }
                        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    private void showTutorialSuggestion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.help_message)
                .setTitle(R.string.help_title)
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                startTutorial();
                                //TODO:tutorial start

                            }
                        });
        builder.setNegativeButton(android.R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();

                    }
                });


        alertDialog = builder.create();
        alertDialog.show();
    }

    private void showCongratulation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.congrat_msg)
                .setTitle(R.string.congrat_title)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                gameModule.getPlayer().release();
                                gameModule.getCurrentInventory().clear();
                                gameModule.getCurrentJournal().clear();
                                gameModule.resetCurrentQuest();
                                selectFragment(questsListFragment, QuestsListFragment.TAG);
                            }
                        });
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void checkAndRequestPermissions() {
        if (!PermissionHelper.hasPermissions(this)) {
            PermissionHelper.requestPermissions(this);
        }
    }

    private boolean isFirstLaunch() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        return prefs.getBoolean(getString(R.string.first_launch), true);
    }

    private void setFirstLaunch(boolean isFirstLauch) {
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).edit();

        editor.putBoolean(getString(R.string.first_launch), isFirstLauch);
        editor.apply();
    }

    private void setUpQuestFragment() {
        placeFragment = new PlaceFragment();

        questFragment = new QuestFragment();
        questFragment.setOnARModeBtnClickListener(getSelectFragmentListener(arFragment));
        questFragment.setOnJournalClickListener(getSelectFragmentListener(journalFragment));
        questFragment.setOnItemClickListener(chooseItemOnClickListener);
        questFragment.setOnPlacesClickListener(getSelectFragmentListener(placeFragment));
        questFragment.setOnInventoryClickListener(getSelectFragmentListener(itemsListFragment));
    }

    private void setUpGameModule() {
        Journal<String> journal = new Journal<>();
        journal.addNow("First record");
        journal.addNow("Second record");
        journal.addNow("Third record");
//        gameModule.addCurrentJournal(journal);

//        gameModule.addCurrentInventory(new Slot(0, Player.INVENTORY, false));

//        Places places = new Places();
//        places.addPlace(new Place(0, "First place", "Description")); //STUB!!!
//        gameModule.addCurrentPlaces(places);
    }

    private void setUpArFragment() throws FileNotFoundException {
        journalFragment = new JournalFragment();

        itemsListFragment = new ItemsListFragment();
        itemsListFragment.setOnItemClickListener(chooseItemOnClickListener);

        Place place = questModule.getNewStyleInteractionDemoPlaceFromScript();

        arFragment = new ARFragment();
        arFragment.setToInventoryOnClickListener(getSelectFragmentListener(itemsListFragment));
        arFragment.setToJournalOnClickListener(getSelectFragmentListener(journalFragment));

        arFragment.setDecorations(place);

        //arFragment.setCloseOnClickListener(onCloseARClickListener);
    }

    public void goToAuthActivity(View v) {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
    }

    public void goARFragment() {
        if (gameModule.getCurrentQuest() == null) {
            Toast.makeText(this, "Сначала выбери квест", Toast.LENGTH_SHORT).show();
            return;
        }
        Place currentPlace = gameModule.getCurrentQuest().getPlaceMap().values().iterator().next();
        gameModule.setCurrentPlace(currentPlace);
        selectFragment(arFragment, ARFragment.TAG);
    }

    public void goToCurrentQuest() {
        if (gameModule.getCurrentQuest() == null) {
            Toast.makeText(this, "Сначала выбери квест", Toast.LENGTH_SHORT).show();
            return;
        }
        Place currentPlace = gameModule.getCurrentQuest().getPlaceMap().values().iterator().next();
        gameModule.setCurrentPlace(currentPlace);
        startService(new Intent(this, GeolocationService.class).putExtra(getString(R.string.foreground),
                isForegroundTracking()));
        selectFragment(questFragment, QuestFragment.TAG);
    }

    public void startTutorial() {
        selectFragment(questsListFragment, QuestsListFragment.TAG);
//        questsListFragment.refreshItems();
        hintModule.clearHintShowHistory();
        hintModule.clearHints();
        hintModule.setEnabled(true);
        setUpTutorial();
    }

    private <F extends Fragment> View.OnClickListener getSelectFragmentListener(final F fragment) {
        if (fragment == null) {
            throw new RuntimeException("tried to register null fragment");
        }
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFragment(fragment, fragment.getClass().getSimpleName());
            }
        };
    }

    private void selectFragment(Fragment fragment, String tag) {
        selectFragment(fragment, tag, true);
    }

    private void selectFragmentByView(Fragment fragment, String tag) {
        selectFragment(fragment, tag, false);
    }

    private void selectFragment(Fragment fragment, String tag, boolean fromNav) {
        showOrHideBars(tag);

        FragmentManager fragmentManager = getSupportFragmentManager();
        int index = fragmentManager.getBackStackEntryCount() - 1;

        boolean needAdd = true;
        if (index >= 0) {
            if (isFragmentInBackstack(fragmentManager,tag)) {
                fragmentManager.popBackStackImmediate(tag, 0);
                needAdd = false;
            }
        }

        //if (fromNav) {
        //    for (int entry = 0; entry < fragmentManager.getBackStackEntryCount(); entry++) {
        //        fragmentManager.popBackStack();
        //    }
        //}

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment_container, fragment, tag);

        if (needAdd) {
            fragmentTransaction.addToBackStack(tag);
        }
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
        setBottomNavItemColor(tag);
        setToolBarByFragment(tag);
    }

    private void setBottomNavItemColor(String fragmentTag) {
        bottomNavigationView.setOnNavigationItemSelectedListener(null);
        if (QuestsListFragment.TAG.equals(fragmentTag)) {
            bottomNavigationView.setSelectedItemId(R.id.action_quests);

        } else if (QuestFragment.TAG.equals(fragmentTag)) {
            bottomNavigationView.setSelectedItemId(R.id.action_current_quest);

        } else if (JournalFragment.TAG.equals(fragmentTag)) {
            bottomNavigationView.setSelectedItemId(R.id.action_current_quest);

        } else if (ItemsListFragment.TAG.equals(fragmentTag)) {
            bottomNavigationView.setSelectedItemId(R.id.action_current_quest);

        } else if (PlaceFragment.TAG.equals(fragmentTag)) {
            bottomNavigationView.setSelectedItemId(R.id.action_current_quest);

        } else if (ARFragment.TAG.equals(fragmentTag)) {
            bottomNavigationView.setSelectedItemId(R.id.action_ar);

        } else if (SettingsFragment.TAG.equals(fragmentTag)) {
            bottomNavigationView.setSelectedItemId(R.id.action_settings);
        }
        bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);
    }

    private void setToolBarByFragment(String fragmentTag) {
        if (QuestsListFragment.TAG.equals(fragmentTag)) {
            setToolBarTitle(getString(R.string.quest_list_fragment_title));

        } else if (QuestFragment.TAG.equals(fragmentTag)) {
            setToolBarTitle(getString(R.string.quest_fragment_title));

        } else if (JournalFragment.TAG.equals(fragmentTag)) {
            setToolBarTitle(getString(R.string.journal_fragment_title));
            goBackByNavigationIcon();

        } else if (ItemsListFragment.TAG.equals(fragmentTag)) {
            setToolBarTitle(getString(R.string.items_list_fragment));
            goBackByNavigationIcon();

        } else if (PlaceFragment.TAG.equals(fragmentTag)) {
            setToolBarTitle(getString(R.string.place_fragment_title));
            goBackByNavigationIcon();

        } else if (SettingsFragment.TAG.equals(fragmentTag)) {
            setToolBarTitle(getString(R.string.settings_fragment_title));
        }
    }

    private void showOrHideBars(String tag) {
        if (tag.equals(ARFragment.TAG) || tag.equals(PlaceFragment.TAG)
                || tag.equals(JournalFragment.TAG) || tag.equals(ItemsListFragment.TAG)) {
            bottomNavigationView.setVisibility(View.GONE);
            if (tag.equals(ARFragment.TAG)) {
                toolBar.setVisibility(View.GONE);
            } else {
                toolBar.setVisibility(View.VISIBLE);
            }
        } else {
            bottomNavigationView.setVisibility(View.VISIBLE);
            toolBar.setVisibility(View.VISIBLE);
        }
    }

    private void showBottomNavigation() {
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    private void goBackByNavigationIcon() {
        toolBar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
                clearToolbarNavigation();
            }
        });
    }

    private void clearToolbarNavigation() {
        toolBar.setNavigationIcon(null);
        toolBar.setNavigationOnClickListener(null);
    }

    private static boolean isFragmentInBackstack(final FragmentManager fragmentManager, final String fragmentTagName) {
        for (int entry = 0; entry < fragmentManager.getBackStackEntryCount(); entry++) {
            if (fragmentTagName.equals(fragmentManager.getBackStackEntryAt(entry).getName())) {
                return true;
            }
        }
        return false;
    }

    private void setToolBarTitle(String title) {
        toolBar.setTitle(title);
    }

    private void checkAuthorization() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        String jwt = prefs.getString(getResources().getString(R.string.json_web_token), null);

        if (jwt == null) {
            Intent intentAuth = new Intent(this, AuthActivity.class);
            startActivity(intentAuth);
        } else {
            networkModule.setToken(jwt);
        }
    }

    private void removeToken() {
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).edit();

        editor.putString(getResources().getString(R.string.json_web_token), null);
        editor.apply();
    }

    private void setUpTutorial() {
        questsListFragment.loadItems(questModule.getQuests());
        hintModule.addHint(R.id.start_ar_hint, new HintModule.NoCompleteHint() {
            @Override
            public void setUpHint(ShowcaseView sv) {
                sv.setTarget(new ViewTarget(
                        bottomNavigationView.findViewById(R.id.action_ar)
                ));
                sv.setContentText(getString(R.string.start_ar_str));
            }
        });
        hintModule.requestHint(R.id.select_quest_hint_name);
    }

    private boolean isForegroundTracking() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        return prefs.getBoolean(getString(R.string.foreground_tracking), true);
    }

    public static final String MESSAGE_PROGRESS = "message_progress";

    private void startDownload(int id) {
        Intent intent = new Intent(this,DownloadService.class);
        intent.putExtra(getResources().getString(R.string.quest_id),id);
        startService(intent);
    }

    private void registerDownloadReceiver() {
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MESSAGE_PROGRESS);
        bManager.registerReceiver(broadcastReceiver, intentFilter);

    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MESSAGE_PROGRESS)) {
                Download download = intent.getParcelableExtra(getResources().getString(R.string.download));
                questsListFragment.setDownloadProgress(download.getId(), download.getProgress());
                System.out.println(download.getProgress());
                if(download.getProgress() == 100) {
                    questsListFragment.setDownloadCompleted(download.getId());
                }
            }
        }
    };
}

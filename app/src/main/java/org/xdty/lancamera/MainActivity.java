package org.xdty.lancamera;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ExpandableDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.xdty.lancamera.data.HistoryService;
import org.xdty.lancamera.module.History;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import tcking.github.com.giraffeplayer.GiraffePlayer;

import static org.xdty.lancamera.R.string.history;

public class MainActivity extends AppCompatActivity implements Drawer.OnDrawerItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final static int ID_LIVE = 1;
    private final static int ID_HISTORY = 2;
    private final static int ID_SETTING = 3;
    private GiraffePlayer player;
    private SharedPreferences mPrefs;
    private String mAuth;
    private List<IDrawerItem> mHistoryItems = new ArrayList<>();
    private ExpandableDrawerItem mHistoryItem;
    private Drawer mDrawer;
    private HistoryService mHistoryService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PrimaryDrawerItem liveItem = new PrimaryDrawerItem().withName(R.string.live)
                .withIdentifier(ID_LIVE);
        mHistoryItem = new ExpandableDrawerItem().withName(history)
                .withSubItems(mHistoryItems)
                .withIdentifier(ID_HISTORY);
        PrimaryDrawerItem settingItem = new PrimaryDrawerItem().withName(R.string.setting)
                .withIdentifier(ID_SETTING);

        mDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withRootView(R.id.container)
                .withActionBarDrawerToggle(true)
                .withActionBarDrawerToggleAnimated(true)
                .addDrawerItems(liveItem)
                .addDrawerItems(mHistoryItem)
                .addDrawerItems(new DividerDrawerItem())
                .addDrawerItems(settingItem)
                .withOnDrawerItemClickListener(this)
                .build();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        player = new GiraffePlayer(this);

        playLive();

        prepareHistory();
    }

    private void playLive() {
        String url = mPrefs.getString(getString(R.string.server_address_key), "");
        //String url = "rtsp://218.204.223.237:554/live/1/66251FC11353191F/e7ooqwcfbqjoo80j.sdp";

        if (!TextUtils.isEmpty(url)) {
            player.play(url);
            player.setTitle(url);
        }
    }

    private void playHistory(History history) {
        String url = mPrefs.getString(getString(R.string.history_address_key), "");

        if (!TextUtils.isEmpty(url)) {
            url += "/" + history.getPath() + "/" + history.getName();
            player.play(url);
            player.setTitle(url);
        }
    }

    private void prepareHistory() {

        String historyUrl = mPrefs.getString(getString(R.string.history_address_key), "");
        if (!TextUtils.isEmpty(historyUrl)) {

            if (!historyUrl.endsWith("/")) {
                historyUrl += "/";
            }

            try {
                URL u = new URL(historyUrl);
                mAuth = Utils.basic(u.getUserInfo());
                String port = String.valueOf(u.getPort() != -1 ? u.getPort() : u.getDefaultPort());
                historyUrl = u.getProtocol() + "://" + u.getHost() + ":" + port + u.getPath();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(historyUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            mHistoryService = retrofit.create(HistoryService.class);

            fetchHistory(mHistoryItem, mHistoryItems, "/");
        }
    }

    private void fetchHistory(final IDrawerItem item, final List<IDrawerItem> subItems,
            final String path) {

        mHistoryService.getHistory(mAuth, path).enqueue(
                new Callback<List<History>>() {
                    @Override
                    public void onResponse(Call<List<History>> call,
                            Response<List<History>> response) {
                        subItems.clear();
                        List<History> histories = response.body();
                        for (int i = histories.size() - 1; i >= 0; i--) {
                            History history = histories.get(i);
                            history.setPath(path);

                            IDrawerItem drawerItem;

                            if (history.getType() == History.Type.DIRECTORY) {
                                ExpandableDrawerItem directoryItem = new ExpandableDrawerItem()
                                        .withName(history.getName())
                                        .withTag(history);

                                List<IDrawerItem> items = directoryItem.getSubItems();
                                if (items == null) {
                                    items = new ArrayList<>();
                                    directoryItem.withSubItems(items);
                                }
                                fetchHistory(directoryItem, items, history.getName());
                                drawerItem = directoryItem;
                            } else {
                                drawerItem = new SecondaryDrawerItem()
                                        .withName(history.getName())
                                        .withTag(history);
                            }
                            subItems.add(drawerItem);

                        }
                        mDrawer.updateItem(item);
                    }

                    @Override
                    public void onFailure(Call<List<History>> call, Throwable t) {
                        Log.e(TAG, t.toString());
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.setting:
                launchSetting();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void launchSetting() {

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.onDestroy();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (player != null) {
            player.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onBackPressed() {
        if (player != null && player.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
        int id = (int) drawerItem.getIdentifier();
        switch (id) {
            case ID_LIVE:
                playLive();
                break;
            case ID_HISTORY:
                break;
            case ID_SETTING:
                launchSetting();
                break;
            default:
                History history = (History) drawerItem.getTag();
                switch (history.getType()) {
                    case DIRECTORY:
                        break;
                    case FILE:
                        playHistory(history);
                        break;
                }
                break;
        }

        return false;
    }
}

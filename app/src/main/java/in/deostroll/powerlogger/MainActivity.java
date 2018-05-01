package in.deostroll.powerlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;

import java.util.List;

import in.deostroll.powerlogger.database.DaoMaster;
import in.deostroll.powerlogger.database.DaoSession;
import in.deostroll.powerlogger.database.LogEntry;
import in.deostroll.powerlogger.database.LogEntryDao;

public class MainActivity extends AppCompatActivity {

    private static final Logger _log = Logger.init("MAT");

    private BroadcastReceiver mRefreshIntentReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FetchRecords();
        Switch swPublish = (Switch) findViewById(R.id.swPublish);
        final SharedPreferences prefs = getApplicationContext().getSharedPreferences("prefs", MODE_PRIVATE);

        swPublish.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("publish", isChecked);
                editor.commit();
                _log.debug("shared pref - publish set: " + isChecked);
            }
        });

        boolean publishFlag = prefs.getBoolean("publish", false);

        _log.debug("shared pref - publish get: " + publishFlag);
        swPublish.setChecked(publishFlag);

        Button btnSetUrl = (Button)findViewById(R.id.btnSetUrl);
        btnSetUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent setUrlIntent = new Intent(MainActivity.this, SetUrlActivity.class);
                startActivity(setUrlIntent);
            }
        });

    }

    @Override
    protected void onResume() {
        _log.debug("onResume()");
        registerForRefreshIntent();
        super.onResume();
    }

    @Override
    protected void onPause() {
        _log.debug("onPause()");
        unregisterForRefreshIntent();
        super.onPause();
    }

    void FetchRecords() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "logs-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
        LogEntryDao EntryDao = daoSession.getLogEntryDao();

        List<LogEntry> entries = EntryDao.queryBuilder().orderDesc(LogEntryDao.Properties.Timestamp).limit(7).list();

        ListView lvRecords = (ListView) this.findViewById(R.id.lvRecords);
        EntryAdapter adapter = new EntryAdapter(getApplicationContext(), entries);
        lvRecords.setAdapter(adapter);
//        Log.d("APL:MAT", "Loaded...");
        _log.info("Fetched items...");
    }

    void registerForRefreshIntent() {
        IntentFilter filter = new IntentFilter(Constants.ACTION_UI_REFRESH);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent != null && intent.getAction().equals(Constants.ACTION_UI_REFRESH)) {
                    FetchRecords();
                }
            }
        };
        registerReceiver(receiver, filter);
        mRefreshIntentReceiver = receiver;
//        Log.d("APL:MAT", "Registered for refresh intent");
        _log.debug("Registered for refresh intent");
    }

    void unregisterForRefreshIntent() {
        if(mRefreshIntentReceiver != null) {
            unregisterReceiver(mRefreshIntentReceiver);
            mRefreshIntentReceiver = null;
//            Log.d("APL:MAT", "Unregistered for refresh intent");
            _log.debug("Unregistered for refresh intent");
        }
    }

}

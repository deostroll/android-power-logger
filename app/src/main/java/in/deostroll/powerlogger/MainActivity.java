package in.deostroll.powerlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import java.util.List;

import in.deostroll.powerlogger.database.DaoMaster;
import in.deostroll.powerlogger.database.DaoSession;
import in.deostroll.powerlogger.database.LogEntry;
import in.deostroll.powerlogger.database.LogEntryDao;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_REFRESH = "in.deostroll.powerlogger.refresh";

    private BroadcastReceiver mRefreshIntentReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FetchRecords();
    }

    @Override
    protected void onResume() {
        Log.d("APL:MAT", "Resumed");
        registerForRefreshIntent();
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d("APL:MAT", "Paused");
        unregisterForRefreshIntent();
        super.onPause();
    }

    void FetchRecords(){
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "logs-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
        LogEntryDao EntryDao = daoSession.getLogEntryDao();

        List<LogEntry> entries = EntryDao.queryBuilder().orderDesc(LogEntryDao.Properties.Timestamp).limit(7).list();

        ListView lvRecords = (ListView) this.findViewById(R.id.lvRecords);
        EntryAdapter adapter = new EntryAdapter(getApplicationContext(), entries);
        lvRecords.setAdapter(adapter);
        Log.d("APL:MAT", "Loaded...");
    }

    void registerForRefreshIntent() {
        IntentFilter filter = new IntentFilter(ACTION_REFRESH);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent != null && intent.getAction().equals(ACTION_REFRESH)) {
                    FetchRecords();
                }
            }
        };
        registerReceiver(receiver, filter);
        mRefreshIntentReceiver = receiver;
        Log.d("APL:MAT", "Registered for refresh intent");
    }

    void unregisterForRefreshIntent() {
        if(mRefreshIntentReceiver != null) {
            unregisterReceiver(mRefreshIntentReceiver);
            mRefreshIntentReceiver = null;
            Log.d("APL:MAT", "Unregistered for refresh intent");
        }
    }
}

package in.deostroll.powerlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.greendao.database.Database;

import java.util.Date;

import in.deostroll.powerlogger.database.DaoMaster;
import in.deostroll.powerlogger.database.DaoSession;
import in.deostroll.powerlogger.database.LogEntry;
import in.deostroll.powerlogger.database.LogEntryDao;

public class PowerChangeReceiver extends BroadcastReceiver {

    private static final String POWER_CONNECTED = "android.intent.action.ACTION_POWER_CONNECTED";

    private static final String ACTION_POST = "in.deostroll.powerlogger.httppost";
    private static final String EXTRA_ID = "in.deostroll.powerlogger.recordId";

    private static final String ACTION_REFRESH = "in.deostroll.powerlogger.refresh";

    @Override
    public void onReceive(final Context context, Intent intent) {
        String currentAction = intent.getAction();
        String status = null;
        if(currentAction.equals(POWER_CONNECTED)) {
            status = "ON";
        }
        else {
            status = "OFF";
        }
        final String finalStatus = status;

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("APL:PCR", "perform logging");
//                LogPowerChange(context, finalStatus, getBatteryReading(context));
//                Toast.makeText(context, String.format("Status: %s", finalStatus), Toast.LENGTH_SHORT).show();
//            }
//        }, 1000);
//        Log.d("APL:PCR", "queued for logging");

        LogPowerChange(context, finalStatus, getBatteryReading(context));
        Toast.makeText(context, String.format("Status: %s", finalStatus), Toast.LENGTH_SHORT).show();


    }

    public void LogPowerChange(final Context context, String status, int batteryReading) {

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, "logs-db");
        Database db = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(db).newSession();
        LogEntryDao entryDao = daoSession.getLogEntryDao();

        LogEntry entry = new LogEntry();
        entry.setPowerStatus(status);
        entry.setBatteryReading(batteryReading);
        entry.setTimestamp(new Date());
        entry.setIsSynched(false);

        final long recordId = entryDao.insert(entry);
        Log.d("APL:PCR", "Inserted record");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent svcIntent = new Intent(context, HttpPushService.class);
                svcIntent.setAction(ACTION_POST);
                svcIntent.putExtra(EXTRA_ID, recordId);
                context.startService(svcIntent);
                Log.d("APL:PCR", "Started the HPS Service");
            }
        }, 1500);

        Intent refreshIntent = new Intent();
        refreshIntent.setAction(ACTION_REFRESH);
        context.sendBroadcast(refreshIntent);

    }

    public int getBatteryReading(Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }
}

package in.deostroll.powerlogger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

    static Logger _log = Logger.init("PCR");

    @Override
    public void onReceive(Context context, Intent intent) {
        String currentAction = intent.getAction();
        String status = null;
        if(currentAction.equals(Constants.POWER_CONNECTED)) {
            status = "ON";
        }
        else {
            status = "OFF";
        }

        LogPowerChange(context, status, getBatteryReading(context));
        Toast.makeText(context, String.format("Status: %s", status), Toast.LENGTH_SHORT).show();
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

        long recordId = entryDao.insert(entry);
        _log.debug("Inserted record: " + recordId);

        Intent refreshIntent = new Intent(Constants.ACTION_UI_REFRESH);
        context.sendBroadcast(refreshIntent);

        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);

        boolean isServiceStarted = prefs.getBoolean("isServiceStarted", false);

        if(!AlarmDispatchReceiver.isStarted){
            Intent alarmDispatch = new Intent(context, AlarmDispatchReceiver.class);
            context.sendBroadcast(alarmDispatch);
            _log.info("Broadcasted for repeating alarm receiver to pickup");
        }
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

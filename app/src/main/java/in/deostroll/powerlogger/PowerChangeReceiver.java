package in.deostroll.powerlogger;

import android.app.AlarmManager;
import android.app.PendingIntent;
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

    static Logger _log = Logger.init("PCR");

    private static PendingIntent mAlarmIntent;

    private static final String ACTION_POST = "in.deostroll.powerlogger.httppost";
    private static final String EXTRA_ID = "in.deostroll.powerlogger.recordId";

    private static final String ACTION_REFRESH = "in.deostroll.powerlogger.refresh";

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

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        LogPowerChange(context, status, getBatteryReading(context), am);
        Toast.makeText(context, String.format("Status: %s", status), Toast.LENGTH_SHORT).show();
    }

    public void LogPowerChange(Context context, String status, int batteryReading, AlarmManager am) {

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

        if(mAlarmIntent != null) {
            am.cancel(mAlarmIntent);
        }

        Intent wakeLockIntent = new Intent(Constants.ACTION_WAKELOCK_ACQUIRE);
        context.sendBroadcast(wakeLockIntent);

        Intent pingServiceIntent = new Intent(context, PingService.class);
        mAlarmIntent = PendingIntent.getService(context, 1011, pingServiceIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        am.set(AlarmManager.RTC, 15000, mAlarmIntent);

        Intent refreshIntent = new Intent(Constants.ACTION_UI_REFRESH);
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

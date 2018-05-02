package in.deostroll.powerlogger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class AlarmDispatchReceiver extends BroadcastReceiver {
    static Logger _log = Logger.init("ADR");
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        boolean isServiceStarted = prefs.getBoolean("isServiceStarted", false);
        if (!isServiceStarted) {
            SharedPreferences.Editor editor = prefs.edit();
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent operation = PendingIntent.getService(context, 1011, new Intent(context, HttpPushService.class), PendingIntent.FLAG_CANCEL_CURRENT);
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, AlarmManager.INTERVAL_FIFTEEN_MINUTES, operation);
            editor.putBoolean("isServiceStarted", true);
            editor.commit();
            _log.info("Scheduled repeating alarm");
        }
    }
}

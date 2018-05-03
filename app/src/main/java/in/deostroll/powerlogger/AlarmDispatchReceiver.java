package in.deostroll.powerlogger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class AlarmDispatchReceiver extends BroadcastReceiver {

    public static boolean isStarted = false;
    static Logger _log = Logger.init("ADR");
    @Override
    public void onReceive(Context context, Intent intent) {

        if (!isStarted) {

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent operation = PendingIntent.getService(context, 1011, new Intent(context, HttpPushService.class), PendingIntent.FLAG_CANCEL_CURRENT);

            am.setRepeating (
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                    operation);

            PendingIntent clearOperation = PendingIntent.getBroadcast(context, 4011, new Intent(context, ClearLogsReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT);

            am.setRepeating(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + AlarmManager.INTERVAL_DAY,
                    AlarmManager.INTERVAL_DAY,
                    clearOperation);

            isStarted = true;
            _log.info("Scheduled repeating alarm");
        }
    }
}

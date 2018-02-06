package in.deostroll.powerlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PowerChangeReceiver extends BroadcastReceiver {
    private static final String ACTION_ON = "in.deostroll.powerlogger.action.ON";
    private static final String ACTION_OFF = "in.deostroll.powerlogger.action.OFF";
    @Override
    public void onReceive(Context context, Intent intent) {
        String currentAction = intent.getAction();
        Intent i = new Intent(context, PowerLoggerHTTPService.class);
        Log.d("PowerChangeReceiver", "received intent with action: " + currentAction);

        if(currentAction == Intent.ACTION_POWER_CONNECTED) {
            // raise power connected

            i.setAction(ACTION_ON);
        }
        else {
            // raise power disconnected
            i.setAction(ACTION_OFF);
        }
        Log.d("PowerChangeReceiver", "starting service");
        context.startService(i);
    }
}

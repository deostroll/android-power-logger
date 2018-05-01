package in.deostroll.powerlogger;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;


public class WakeLockController extends BroadcastReceiver {

    private static PowerManager.WakeLock lock;
    private static Logger _log = Logger.init("WLC");
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if(action.equals(Constants.ACTION_WAKELOCK_ACQUIRE)){
            if(lock == null) {
                lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.WAKELOCK_TAG);
                lock.acquire();
                _log.debug("Lock created");
                _log.info("wakelock acquired");
            }
            else if(lock.isHeld()) {
                _log.debug("Reference reused");
                _log.debug("wakelock already held");
            }
            else {
                lock.acquire();
                _log.debug("Reference reused");
                _log.info("wakelock acquired");
            }
        }
        else if(action.equals(Constants.ACTION_WAKELOCK_RELEASE)) {
            if(lock != null) {
                if(lock.isHeld()) {
                    lock.release();
                    _log.debug("Reference reused");
                    _log.info("wakelock released");
                }
            }
            else {
                lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.WAKELOCK_TAG);
                if(lock.isHeld()) {
                    lock.release();
                }
            }
        }
    }
}

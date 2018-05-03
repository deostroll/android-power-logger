package in.deostroll.powerlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import in.deostroll.powerlogger.database.LogEntryDao;

public class ClearLogsReceiver extends BroadcastReceiver {
    static Logger _log = Logger.init("CLR");
    @Override
    public void onReceive(Context context, Intent intent) {
        LogEntryDao dao = DataManager.getDao(context);
        dao.queryBuilder().where(LogEntryDao.Properties.IsSynched.eq(true)).buildDelete().executeDeleteWithoutDetachingEntities();
        _log.info("Cleared logs");
    }
}

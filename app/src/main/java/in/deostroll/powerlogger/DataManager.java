package in.deostroll.powerlogger;

import android.content.Context;

import org.greenrobot.greendao.database.Database;

import java.util.List;

import in.deostroll.powerlogger.database.DaoMaster;
import in.deostroll.powerlogger.database.DaoSession;
import in.deostroll.powerlogger.database.LogEntry;
import in.deostroll.powerlogger.database.LogEntryDao;

class DataManager {

    public static List<LogEntry> getNonSynched(Context context) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, "logs-db");
        Database db = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(db).newSession();
        LogEntryDao entryDao = daoSession.getLogEntryDao();
        return entryDao.queryBuilder()
                .where(LogEntryDao.Properties.IsSynched.eq(false))
                .orderAsc(LogEntryDao.Properties.Id)
                .list();
    }

    public static LogEntryDao getDao(Context context) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, "logs-db");
        Database db = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(db).newSession();
        LogEntryDao entryDao = daoSession.getLogEntryDao();
        return entryDao;
    }
}

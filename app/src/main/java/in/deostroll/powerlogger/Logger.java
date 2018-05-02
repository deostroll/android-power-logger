package in.deostroll.powerlogger;

import android.util.Log;

public class Logger {

    private static String APP = "APL";
    private String module;

    private Logger(String module) {
        this.module = module;
    }

    private String getFullModuleName() {
        return String.format("%s:%s", APP, module);
    }

    public void debug(String msg) {
        Log.d(getFullModuleName(), msg);
    }

    public void verbose(String msg) {
        Log.v(getFullModuleName(), msg);
    }

    public static Logger init(String module) {
        Log.d("APL:LGR", "Initialized: " + module);
        return new Logger(module);
    }


    public void info(String msg) {
        Log.i(getFullModuleName(), msg);
    }

    public void error(String message) {
        Log.e(getFullModuleName(), message);
    }
}

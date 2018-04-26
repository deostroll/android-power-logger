package in.deostroll.powerlogger;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.greenrobot.greendao.database.Database;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import in.deostroll.powerlogger.database.DaoMaster;
import in.deostroll.powerlogger.database.DaoSession;
import in.deostroll.powerlogger.database.LogEntry;
import in.deostroll.powerlogger.database.LogEntryDao;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class HttpPushService extends IntentService {

    private static final String ACTION_POST = "in.deostroll.powerlogger.httppost";
    private static final String EXTRA_ID = "in.deostroll.powerlogger.recordId";

    private SimpleDateFormat sdfDate = new SimpleDateFormat("E, dd MMM yyyy");
    private SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");

    private static final String url = "https://script.google.com/macros/s/AKfycbymGQB00rmSf1u-F0vuAV97q0nxvwaqrqhzfojFtp_gDJNHmYs/exec";

    static private RequestQueue queue;

    public HttpPushService() {
        super("HttpPushService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent != null){
            String action = intent.getAction();
            if(action.equals(ACTION_POST)) {
                postData(intent);
            }
            else {
                Log.d("APL:HPS", "Nothing to do...");
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private RequestQueue getRequestQueue(){
        return Volley.newRequestQueue(this);
    }

    private void postData(Intent intent) {

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HPS");

        final long id = intent.getLongExtra(EXTRA_ID, -1);
        if(id != -1 && isNetworkAvailable()) {
            DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "logs-db");
            Database db = helper.getWritableDb();
            DaoSession daoSession = new DaoMaster(db).newSession();
            LogEntryDao entryDao = daoSession.getLogEntryDao();
            LogEntry entry = entryDao.load(id);
            Date entryDate = entry.getTimestamp();

            String dateString = sdfDate.format(entryDate);
            String timeString = sdfTime.format(entryDate);

            JSONObject payload = null;

            try {
                payload = new JSONObject()
                    .accumulate("status", entry.getPowerStatus())
                    .accumulate("battery", entry.getBatteryReading())
                    .accumulate("dateString", dateString)
                    .accumulate("timeString", timeString);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("APL:HPS", "Json Data Format", e);
                return;
            }

//            RequestQueue queue = Volley.newRequestQueue(this);
            Log.v("APL:HPS", String.format("Payload: %s", payload.toString()));
            JsonObjectRequest httpPost = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    payload,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            wakeLock.release();
                            Log.d("APL:HPS", String.format("Response posted for record: %d", id));
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            wakeLock.release();
                            Log.e("APL:HPS", String.format("HttpError while posting record: %d", id), error);
                        }
                    }
            );
            wakeLock.acquire();
            if(queue == null) {
                Log.d("APL:HPS", "Obtaining request queue");
                queue = getRequestQueue();
            }
            queue.add(httpPost);
            Log.d("APL:HPS", "Sending record: " + id);
        }
    }


}

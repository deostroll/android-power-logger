package in.deostroll.powerlogger;

import android.app.IntentService;
import android.content.Intent;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import in.deostroll.powerlogger.database.LogEntry;

public class PingService extends IntentService {
    private static Logger _log = Logger.init("SPR");
    private String url;
    private RequestQueue queue;

    public PingService() {
        super("PingService");
    }
    final int PING_COUNT = 5;
    int SUCCESS_COUNT = 0;
    int ERROR_COUNT = 0;
    int counter = 0;
    Intent currentIntent;
    @Override
    protected void onHandleIntent(Intent intent) {
        queue = Volley.newRequestQueue(this);
        currentIntent = intent;
        _log.info("Ping service started");
        next();
    }

    void next() {
        if(counter < PING_COUNT){
            JsonObjectRequest request = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    SUCCESS_COUNT++;
                    _log.debug(String.format("Trial %d : end success", counter));
                    next();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    ERROR_COUNT++;
                    _log.debug(String.format("Trial %d : end error", counter));
                    next();
                }
            });
            counter++;
            _log.debug(String.format("Trial %d : start", counter));
        }
        else {
            if(ERROR_COUNT == 0) {
//                call the next service
                Intent pushService = new Intent(this, HttpPushService.class);
                List<LogEntry> entries = DataManager.getNonSynched(this);
                ArrayList<Long> recordIds = new ArrayList<Long>();
                for (LogEntry item: entries){
                    recordIds.add(item.getId());
                }
                pushService.putExtra("records", recordIds.toArray());
                startService(pushService);
                _log.debug("Starting the push service");
            }
            else {
//                call this service again
                startService(currentIntent);
                _log.debug("Restarting the ping service");
            }
        }
    }

}

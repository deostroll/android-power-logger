package in.deostroll.powerlogger;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class PowerLoggerHTTPService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS

    private static final String ACTION_ON = "in.deostroll.powerlogger.action.ON";
    private static final String ACTION_OFF = "in.deostroll.powerlogger.action.OFF";

    public PowerLoggerHTTPService() {
        super("PowerLoggerHTTPService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("PowerLoggerHTTPService", "received intent");
        if (intent != null) {
            final String action = intent.getAction();
            if(ACTION_ON.equals(action)) {
                on();
            }
            else {
                off();
            }
        }
    }

    private void on(){
        WebLog("ON");
    }

    private void off() {
        WebLog("OFF");
    }

    private void WebLog(String path) {
        String url = "http://192.168.43.240:3000/power/" + path;
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest req = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
//                simply log it
                Log.d("PowerLoggerHTTPService", "Response: " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("PowerLoggerHTTPService", error.getMessage());
            }
        });

        queue.add(req);
    }
}

package in.deostroll.powerlogger;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import in.deostroll.powerlogger.database.LogEntry;
import in.deostroll.powerlogger.database.LogEntryDao;

public class HttpPushService extends IntentService {

    private static SimpleDateFormat sdfDate = new SimpleDateFormat("E, dd MMM yyyy");
    private static SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");

    private static Logger _log = Logger.init("HPS");
    private Timer tmr;
    public HttpPushService() {
        super("HttpPushService");
    }
    private String url;

    int batchCounter = 0;
    int totalBatches = 0;
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        String DEFAULT_URL = getResources().getString(R.string.script_url);
        url = prefs.getString("url", DEFAULT_URL);

        RequestQueue queue = Volley.newRequestQueue(this);
        final AutoResetEvent evt = new AutoResetEvent(false);
        // 1. Do ping
        RequestQueue.RequestFinishedListener<String> pingListener;
        pingListener = new RequestQueue.RequestFinishedListener<String>() {
            @Override
            public void onRequestFinished(Request<String> request) {
                evt.set();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                _log.info("Pinged");
            }
        };

        queue.addRequestFinishedListener(pingListener);
        doPing(queue);

        try {
            _log.debug("Waiting for ping to complete");
            evt.waitOne();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        queue.removeRequestFinishedListener(pingListener);

        // 2. do upload

        final AutoResetEvent evt2 = new AutoResetEvent(false);



        List<LogEntry> entries = DataManager.getNonSynched(this);
        if(entries.size() == 0) {
            _log.info("Nothing to process");
            return;
        }
        totalBatches = (int) Math.ceil(entries.size()*1.0/Constants.BATCH_SIZE);
        RequestQueue.RequestFinishedListener<JSONObject> uploadedListener = new RequestQueue.RequestFinishedListener<JSONObject>() {
            @Override
            public void onRequestFinished(Request<JSONObject> request) {

                batchCounter++;
                _log.verbose(String.format("Batch count: %d, Total: %d", batchCounter, totalBatches));
                if(batchCounter == totalBatches) {
                    evt2.set();
                }
            }
        };
        queue.addRequestFinishedListener(uploadedListener);

        doUpload(entries, queue);

        try {
            _log.debug("Waiting for upload to finish");
            evt2.waitOne();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        queue.removeRequestFinishedListener(uploadedListener);

        AutoResetEvent evt3 = new AutoResetEvent(false);

        sync(entries, queue, evt3);

        try {
            _log.debug("Waiting for sync");
            evt3.waitOne();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        _log.info("Done with http push");

    }

    private void sync(final List<LogEntry> entries, RequestQueue queue, final AutoResetEvent evt3) {
        String checkUrl = String.format("%s?op=verify&count=%d&_=%d", url, entries.size(), new Date().getTime());
        _log.verbose("checkUrl: " + checkUrl);
        JsonArrayRequest req = new JsonArrayRequest(checkUrl, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                List<Long> ids = new ArrayList<>();
                _log.verbose("Sync response: " + response.toString());
                for(int k = 0, l = response.length(); k < l; k++) {
                    try {
                        ids.add(response.getLong(k));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                LogEntryDao entryDao = DataManager.getDao(HttpPushService.this);

                for(LogEntry item : entries) {
                    Long itemId = item.getId();
                    if(ids.indexOf((Object)itemId) > -1) {
                        item.setIsSynched(true);
                        _log.verbose("Updating status of item: " + itemId);
                        entryDao.update(item);
                    }
                    else {
                        _log.warn("Item not synced: " + itemId);
                    }
                }
                evt3.set();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                _log.error("Sync error: " + Log.getStackTraceString(error));
                evt3.set();
            }
        });
        queue.add(req);
    }

    private void doUpload(List<LogEntry> entries, RequestQueue queue) {
        _log.verbose(String.format("Count: %d", entries.size()));
        JSONArray arr = new JSONArray();

        for (LogEntry item: entries) {
            JSONObject obj = makeJSONObject(item);
            arr.put(obj);
            if(arr.length() == Constants.BATCH_SIZE) {
                uploadBatch(arr, queue);
                arr = new JSONArray();
            }
        }

        if(arr.length() > 0) {
            uploadBatch(arr, queue);
        }

    }

    private void uploadBatch(JSONArray arr, RequestQueue queue) {
        _log.verbose(String.format("Uploading batch: %d", arr.length()));
        try {
            JSONObject payload = new JSONObject()
                    .accumulate("entries", arr);
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, payload, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    _log.verbose("uploadBatchResponse: " + response.toString());
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    _log.error("uploadBatchError: " + Log.getStackTraceString(error));
                }
            });
            queue.add(req);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void doPing(RequestQueue queue) {
        String pingUrl = String.format("%s?op=ping&battery=%d", url, getBatteryReading(this));
        StringRequest req = new StringRequest(Request.Method.GET, pingUrl, null, null);
        queue.add(req);
    }

//    @Override
//    protected void onHandleIntent(@Nullable Intent intent) {
//        if(intent != null) {
//            RequestQueue queue = Volley.newRequestQueue(this);
//            final int[] counters = {0, 0, 0};
//
//            final long[] ids = intent.getLongArrayExtra(Constants.INTENT_FIELD_RECORD_IDS);
//
//            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
//            String url = prefs.getString("url", getResources().getString(R.string.script_url));
//            queue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<JSONObject>() {
//                @Override
//                public void onRequestFinished(Request<JSONObject> request) {
//                    counters[INDEX_FINISHED]++;
//                    _log.verbose(String.format("Count: %d, Finished: %d", counters[INDEX_COUNT], counters[INDEX_FINISHED]));
//                    if(counters[INDEX_COUNT] == counters[INDEX_FINISHED]) {
//                        Intent wakeLockRelease = new Intent(Constants.ACTION_WAKELOCK_RELEASE);
//                        sendBroadcast(wakeLockRelease);
//                        int l = ids.length;
//                        LogEntryDao _entryDao = DataManager.getDao(HttpPushService.this);
//                        for(int k = 0; k < l; k++) {
//                            long _id = ids[k];
//                            LogEntry _entry = _entryDao.load(_id);
//                            _entry.setIsSynched(true);
//                            _entryDao.update(_entry);
//                        }
//                        _log.info("Push service done");
//                    }
//                }
//            });
//
//            JSONArray arr = new JSONArray();
//
//            for(int k = 0; k < ids.length; k++) {
//                long id = ids[k];
//                counters[INDEX_COUNT]++;
//                LogEntryDao entryDao = DataManager.getDao(this);
//                LogEntry entry = entryDao.load(id);
//                JSONObject obj = makeJSONObject(entry);
//                arr.put(obj);
//                if(arr.length() == Constants.BATCH_SIZE) {
//                    post(url, arr, counters, queue);
//                    arr = new JSONArray();
//                }
//            }
//
//            if(arr.length() > 0) {
//                post(url, arr, counters, queue);
//            }
//        }
//    }
//
    private JSONObject makeJSONObject(LogEntry entry) {

        Date ts = entry.getTimestamp();
        String dateString = sdfDate.format(ts);
        String timeString = sdfTime.format(ts);

        try {
            return new JSONObject()
                    .accumulate("id", entry.getId())
                    .accumulate("status", entry.getPowerStatus())
                    .accumulate("battery", entry.getBatteryReading())
                    .accumulate("dateString", dateString)
                    .accumulate("timeString", timeString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
//
//    private void post(String url, JSONArray arr, final int[] counters, RequestQueue queue) {
//        counters[INDEX_COUNT]++;
//        final int batch_count = counters[INDEX_COUNT];
//
//        JSONObject payload = null;
//        try {
//            payload = new JSONObject()
//                    .accumulate("entries", arr)
//                    .accumulate("postedTimestamp", System.currentTimeMillis());
//        } catch (JSONException e) {
//            _log.error(Log.getStackTraceString(e));
//            return;
//        }
//        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, payload, null, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                if(error.networkResponse != null && error.networkResponse.statusCode != 200) {
//                    _log.error("Error while posting batch: " + batch_count);
//                    _log.error(Log.getStackTraceString(error));
//                    counters[INDEX_ERRORS]++;
//                }
//            }
//        });
//        req.setRetryPolicy(new DefaultRetryPolicy(5000 , 0, 0));
//        queue.add(req);
//        _log.debug("Posted batch: " + batch_count);
//    }
//
//    private void post(String url, RequestQueue queue, final int[] counters, final LogEntry entry, final LogEntryDao entryDao) {
//
//        long id = entry.getId();
//
//        JSONObject payload = null;
//        try {
//            payload = new JSONObject()
//                    .accumulate("status", entry.getPowerStatus())
//                    .accumulate("battery", entry.getBatteryReading())
//                    .accumulate("dateString", sdfDate.format(entry.getTimestamp()))
//                    .accumulate("timeString", sdfTime.format(entry.getTimestamp()));
//        } catch (JSONException e) {
//            _log.error(String.format("Error in json : %s", Log.getStackTraceString(e)));
//            return;
//        }
//
//        queue.add(new JsonObjectRequest(Request.Method.POST, url, payload, new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject response) {
//                entry.setIsSynched(true);
//                entryDao.update(entry);
//
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                if (error.networkResponse != null && error.networkResponse.statusCode != 200) {
//                    _log.error(String.format("VolleyError: %s", Log.getStackTraceString(error)));
//                    counters[INDEX_ERRORS]++;
//                }
//            }
//        }).setTag(entry).setRetryPolicy(new DefaultRetryPolicy(5000, 0, 0)));
//    }

    public int getBatteryReading(Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }
}

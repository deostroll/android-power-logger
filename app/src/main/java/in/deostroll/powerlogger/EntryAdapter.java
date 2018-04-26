package in.deostroll.powerlogger;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;

import in.deostroll.powerlogger.database.LogEntry;


public class EntryAdapter extends BaseAdapter {

    private final Context _context;
    List<LogEntry> _theList;
    private final LayoutInflater _inflater;
    private SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy, HH:mm");
    public EntryAdapter(Context context, List<LogEntry> logEntries) {
        _theList = logEntries;
        _inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _context = context;
    }

    @Override
    public int getCount() {
        return _theList.size();
    }

    @Override
    public Object getItem(int position) {
        return _theList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = _inflater.inflate(R.layout.entry_view_template, parent, false);
        TextView tvStatus = (TextView) convertView.findViewById(R.id.tvStatus);
        ImageView ivIcon = (ImageView) convertView.findViewById(R.id.ivPowerIcon);
        TextView tvDateTime = (TextView) convertView.findViewById(R.id.tvDate);
        TextView tvBattery = (TextView) convertView.findViewById(R.id.tvBattery);

        LogEntry entry = (LogEntry) getItem(position);
        String status = entry.getPowerStatus();
        tvBattery.setText(String.format("Battery: %d %s", entry.getBatteryReading(), "%"));
        tvStatus.setText(status);
        tvDateTime.setText(sdf.format(entry.getTimestamp()));

        Drawable drawable;
        if(status.equals("ON")){
            drawable = ContextCompat.getDrawable(_context, R.mipmap.ic_power_on);
        }
        else {
            drawable = ContextCompat.getDrawable(_context, R.mipmap.ic_power_off);
        }
        ivIcon.setImageDrawable(drawable);
        if (position % 2 != 0) {
            convertView.setBackgroundColor(Color.LTGRAY);
        }
        return convertView;
    }
}

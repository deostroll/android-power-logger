package in.deostroll.powerlogger;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SetUrlActivity extends AppCompatActivity {

    static Logger _log = Logger.init("SUA");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_url);

        final SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        String url = prefs.getString("url", getResources().getString(R.string.script_url));

        Button btnDone = (Button) findViewById(R.id.btnDone);
        Button btnSet = (Button)findViewById(R.id.btnSet);
        final TextView tvCurrentUrl = (TextView)findViewById(R.id.tvCurrentUrl);
        final EditText etUrl = (EditText)findViewById(R.id.etUrl);

        etUrl.setText(url);
        tvCurrentUrl.setText(url);

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _log.info("Done with SetUrlActivity");
                finish();
            }
        });


        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String finalText = etUrl.getText().toString();
                prefs.edit().putString("url",finalText).commit();
                tvCurrentUrl.setText(finalText);
                Toast.makeText(SetUrlActivity.this, "Done", Toast.LENGTH_SHORT).show();
                _log.info("Url changed");
            }
        });


    }
}

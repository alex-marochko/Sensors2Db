package name.marochko.sensors2db;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    final String LOG_TAG = "marinfo";
    private boolean bound;
    private TextView sensorTextView;
    private Button btnStartRecording, btnStopRecording, btnClearDB, btnReadDB;
    private FromSensorsToDB all_staff;// = new FromSensorsToDB(this);
    private Intent intent;
    ServiceConnection sConn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "MainActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        intent = new Intent(this, FromSensorsToDB.class);

        sConn = new ServiceConnection() {

            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d(LOG_TAG, "MainActivity onServiceConnected");
                all_staff = ((FromSensorsToDB.MyBinder) binder).getService();
                bound = true;
                Log.d(LOG_TAG, "MainActivity onServiceConnected all_staff: " + all_staff);
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.d(LOG_TAG, "MainActivity onServiceDisconnected");
                bound = false;
            }
        };


        btnStartRecording = (Button)findViewById(R.id.btnStartRecording);
        btnStopRecording = (Button)findViewById(R.id.btnStopRecording);
        btnClearDB = (Button)findViewById(R.id.btnClearDB);
        btnReadDB  = (Button)findViewById(R.id.btnShowDB);

    }


    @Override
    protected void onStart() {
        Log.d(LOG_TAG, "MainActivity.onStart");
        super.onStart();
        bindService(intent, sConn, 0);
        startService(intent);
    }

    @Override
    protected void onStop() {
        Log.d(LOG_TAG, "MainActivity.onStop");
        super.onStop();
        if (!bound) return;
        unbindService(sConn);
        bound = false;
    }


/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
*/


    public void onStartClick(View v) {
        Log.d(LOG_TAG, "MainActivity.onStartClick");

        btnStopRecording.setEnabled(true);
        btnStartRecording.setEnabled(false);
        btnClearDB.setEnabled(false);

        all_staff.startSensors();
    }

    public void onStopClick(View v){
        Log.d(LOG_TAG, "MainActivity.onStopClick");

        all_staff.stopSensors();

        btnStopRecording.setEnabled(false);
        btnStartRecording.setEnabled(true);
        btnClearDB.setEnabled(true);

//        all_staff.stopService(intent);

//        stopService(intent);

    }

    public void onExitClick(View v){

        Log.d(LOG_TAG, "MainActivity.onExitClick");

        all_staff.stopService();
        this.finish();
        System.exit(0);

    }

    public void onShowClick(View v){
        Log.d(LOG_TAG, "MainActivity.onShowClick");

        all_staff.readDB();

    }

    public void onClearClick(View v){
        Log.d(LOG_TAG, "MainActivity.onClearClick");

        all_staff.clearDB();

    }


}

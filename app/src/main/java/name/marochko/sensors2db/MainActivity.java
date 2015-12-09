package name.marochko.sensors2db;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    final String LOG_TAG = "marinfo";
    private boolean bound;
    private TextView sensorTextView;
    private Button btnStartRecording, btnStopRecording, btnClearDB, btnReadDB;
    private FromSensorsToDB all_staff;// = new FromSensorsToDB(this);
    private Intent intent;
    ServiceConnection sConn;
    private ListView lvSensors;
    private ArrayAdapter<String> adapter;

    private int sensorsCount;
    private int sensorsDelay = 0;

    List<Sensor> sensorsList = new LinkedList<>();


    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        Log.d(LOG_TAG, "MainActivity.onCreate");
        if (savedInstanceState != null) Log.d(LOG_TAG, "savedInstanceState (in OnCreate):\n" + savedInstanceState.toString());
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

            }
        });
*/

        intent = new Intent(this, FromSensorsToDB.class);

        lvSensors = (ListView)findViewById(R.id.lvSensors);
//        lvSensors.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);


        sConn = new ServiceConnection() {

            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d(LOG_TAG, "MainActivity onServiceConnected");
                all_staff = ((FromSensorsToDB.MyBinder) binder).getService();
                bound = true;
                Log.d(LOG_TAG, "MainActivity onServiceConnected all_staff: " + all_staff);

                sensorsCount = all_staff.loadSensorsList().size();
                sensorsList = all_staff.loadSensorsList();

                String[] sensorsNames = new String[sensorsCount];
                for (int i = 0; i <= (sensorsCount - 1); i++)
                    sensorsNames[i] = all_staff.loadSensorsList().get(i).getName();
                adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_multiple_choice, sensorsNames);
                lvSensors.setAdapter(adapter);

            }

            public void onServiceDisconnected(ComponentName name) {
                Log.d(LOG_TAG, "MainActivity onServiceDisconnected");
                bound = false;
            }
        };

        btnStartRecording = (Button)findViewById(R.id.btnStartRecording);
        btnStopRecording = (Button)findViewById(R.id.btnStopRecording);
        btnClearDB = (Button)findViewById(R.id.btnClearDB);

        // адаптер

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.strArrSensorsDelay));

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner = (Spinner) findViewById(R.id.spinnerSensorsRate);
        spinner.setAdapter(adapter);
        // заголовок
        spinner.setPrompt(getResources().getString(R.string.spinnerSensorsDelayPromt));
        // выделяем элемент
        spinner.setSelection(0);
        // устанавливаем обработчик нажатия
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                // показываем позиция нажатого элемента
//                Toast.makeText(getBaseContext(), "Position = " + position, Toast.LENGTH_SHORT).show();
                sensorsDelay = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState){

        super.onSaveInstanceState(outState);

        Log.d(LOG_TAG, "onSaveInstanceState:\n" + outState.toString());


    }


    @Override
    protected void onStart() {
        Log.d(LOG_TAG, "MainActivity.onStart");
        super.onStart();
        bindService(intent, sConn, 0);
        startService(intent);

/*
        int sensorsCount = all_staff.loadSensorsList().size();
        String[] sensorsNames = new String[sensorsCount];
        for(int i=0; i<=sensorsCount; i++) sensorsNames[i] = all_staff.loadSensorsList().get(i).getName();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, sensorsNames);
        lvSensors.setAdapter(adapter);
*/

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

        all_staff.startSensors(loadCheckedSensorsList(), sensorsDelay);
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

    public void onShowSensorsListClick(View v){



        for(Sensor s: sensorsList) Log.d(LOG_TAG, s.toString() + '\n');

    }

    public void onExportDBClick(View v){

        all_staff.exportDB();

    }

    private int[] loadCheckedSensorsList(){

        int[] selectedSensors;



        SparseBooleanArray sbCheckedArray = lvSensors.getCheckedItemPositions();

        selectedSensors = new int[sbCheckedArray.size()];

        for(int i = 0; i < sbCheckedArray.size(); i++){

            if(sbCheckedArray.get(sbCheckedArray.keyAt(i))) {

                Log.d(LOG_TAG, "sbCheckedArray.size() = " + sbCheckedArray.size());

                Log.d(LOG_TAG, "sbCheckedArray.keyAt(i) = " + sbCheckedArray.keyAt(i));

                Log.d(LOG_TAG, "sensorsList.size()" + sensorsList.size());

//                if(sensorsList.get(sbCheckedArray.keyAt(i)).isWakeUpSensor())
                selectedSensors[i] = sensorsList.get(sbCheckedArray.keyAt(i)).getType();
            }
        }

        return selectedSensors;

    }



}

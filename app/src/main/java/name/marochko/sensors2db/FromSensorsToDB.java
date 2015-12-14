package name.marochko.sensors2db;

/**
 * This class reads sensors and writes data to database
 */

import android.app.Notification;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class FromSensorsToDB extends Service implements SensorEventListener {

    private final String LOG_TAG ="marinfo";

    private final long PERIOD_FOR_REGULAR_COMMIT = 1_000_000_000; //one second (in nanoseconds)

    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private SensorManager mSensorManager;
    private Sensor[] mSensor;
    private Context context;

    MyBinder binder = new MyBinder();

    private boolean transaction_flag = false;
    int count = 0;
    long time = 0;
    long previousTimestamp = time;

    ContentValues contentValues = new ContentValues();



    public void startSensors( int[] sensors, int sensorsDelay){

        mSensor = new Sensor[sensors.length];

        for(int i=0; i<sensors.length; i++) {
            mSensor[i] = mSensorManager.getDefaultSensor(sensors[i]);
            mSensorManager.registerListener(this, mSensor[i], sensorsDelay);
        }

        db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        transaction_flag = true;
    }


    public void stopSensors(){

        Log.d(LOG_TAG, "FromSensorsToDB.stopSensors()");

        mSensorManager.unregisterListener(this);

        Log.d(LOG_TAG, "FromSensorsToDB.stopSensors()- unregisterListener ");

        db.setTransactionSuccessful();

        Log.d(LOG_TAG, "FromSensorsToDB.stopSensors()- setTransactionSuccessful() ");

        db.endTransaction();
        transaction_flag = false;
        dbHelper.close();

//        stopSelf();

    }

    public void stopService(){
        Log.d(LOG_TAG, "FromSensorsToDB.stopService()");

        stopSelf();
    }

    public void readDB(){

        Log.d(LOG_TAG, "FromSensorsToDB.readDB()");

        db = dbHelper.getWritableDatabase();

        Cursor c = db.rawQuery("select * from sensors_data", null);
        if (c != null) {
            Log.d(LOG_TAG, "Records count = " + c.getCount());
            if (c.moveToFirst()) {
                do {
                    Log.d(LOG_TAG, c.getString(c.getColumnIndex("x")));
                } while (c.moveToNext());
            }
            c.close();
        }

        dbHelper.close();


    }

    public void exportDB(){

        Log.d(LOG_TAG, "exportDB()");

        final String  DIR_SD = "Sensors2Db";
        final String  FILENAME_SD = "sensors.csv";

        db = dbHelper.getReadableDatabase();

        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Log.d(LOG_TAG, "SD-card is not available: " + Environment.getExternalStorageState());
            return;
        }

        File sdPath = Environment.getExternalStorageDirectory();
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);

        sdPath.mkdirs();
        File sdFile = new File(sdPath, FILENAME_SD);

        Log.d(LOG_TAG, "File: " + sdFile);


        try{

        FileWriter fw = new FileWriter(sdFile);

            fw.write("id;sens_type;x;y;z;time\n");

        Cursor c = db.rawQuery("select * from sensors_data", null);
        if (c != null) {

            if (c.moveToFirst()) {
                do {
                    fw.write(c.getString(c.getColumnIndex("id")) + ";" +

                            c.getString(c.getColumnIndex("sens_type")) + ";" +
                                    c.getString(c.getColumnIndex("x")) + ";" +
                                    c.getString(c.getColumnIndex("y")) + ";" +
                                    c.getString(c.getColumnIndex("z")) + ";" +
                                    c.getString(c.getColumnIndex("time")) + "\n"

                    );
                } while (c.moveToNext());
            }
            fw.close();
            c.close();
        }

            Toast.makeText(getApplicationContext(), "CSV file is saved to " + sdFile, Toast.LENGTH_LONG).show();

        }catch (IOException e){

            Log.e(LOG_TAG, "IOException: " + e.getMessage());

        }

        dbHelper.close();


    }

    public void clearDB(){

        Log.d(LOG_TAG, "FromSensorsToDB.clearDB");

        db = dbHelper.getWritableDatabase();

        db.delete("sensors_data", null, null);

        dbHelper.close();


    }

    public List<Sensor> loadSensorsList(){

        return mSensorManager.getSensorList(Sensor.TYPE_ALL);

    }



    public void onAccuracyChanged(Sensor sensor, int accuracy){

        Log.d(LOG_TAG, "FromSensorsToDB.onAccuracyChanged()");
    }


    public void onSensorChanged(SensorEvent event){

        Log.d(LOG_TAG, "FromSensorsToDB.onSensorChanged");

        if(!transaction_flag) return;

        time = event.timestamp;
        if(previousTimestamp == 0)previousTimestamp = time;

        float x,y,z;

        count++;

        x = event.values[0];
        y = event.values[1];
        z = event.values[2];


            contentValues.clear();

            contentValues.put("sens_type", event.sensor.getName());
            contentValues.put("x", x);
            contentValues.put("y", y);
            contentValues.put("z", z);
            contentValues.put("time", time);

            Log.d(LOG_TAG, contentValues.getAsString("x"));

            db.insert("sensors_data", null, contentValues);

            Log.d(LOG_TAG, "record inserted!");

//            Log.d(LOG_TAG, "Checking commit conditions: " + (previousTimestamp!=0)+  " and " + ((time - previousTimestamp)>PERIOD_FOR_REGULAR_COMMIT));



            if((previousTimestamp!=0) & ((time - previousTimestamp)>PERIOD_FOR_REGULAR_COMMIT)){

                //if PERIOD_FOR_REGULAR_COMMIT expired then commit and start new transaction:
                db.setTransactionSuccessful();
                db.endTransaction();
                db.beginTransaction();
                previousTimestamp = time;
//                Log.d(LOG_TAG, "Commit");
            }
    }

    protected void startForeground(){

        Log.d(LOG_TAG, "FromSensorsToDB.startForeground()");
        Notification notification = new Notification();


        startForeground(R.drawable.sensors2db, notification);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(LOG_TAG, "FromSensorsToDB.onStartCommand");

        startForeground();

        dbHelper = new DBHelper(this);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);



//        startSensors();



        return super.onStartCommand(intent, flags, startId);
    }

    class DBHelper extends SQLiteOpenHelper{

        public DBHelper(Context context){
            super(context, "sensorDB", null, 1);
        }

        public void onCreate(SQLiteDatabase db){

            Log.d(LOG_TAG, "FromSensorsToDB.DBHelper.onCreate");

            db.execSQL("create table sensors_data(" +
                            "id integer primary key autoincrement," +
                            "sens_type text, x real, y real, z real, time long" +
                            ");"
            );
        }
        public void onUpgrade(SQLiteDatabase db, int oldversion, int newversion){

            Log.d(LOG_TAG, "FromSensorsToDB.DBHelper.onUpgrade");

        }
    }

    public IBinder onBind(Intent arg0) {
        Log.d(LOG_TAG, "MyService onBind");
        return binder;
    }

    class MyBinder extends Binder {
        FromSensorsToDB getService() {
            Log.d(LOG_TAG, "MyBinder.getService()");
            return FromSensorsToDB.this;
        }
    }

}



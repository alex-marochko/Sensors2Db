package name.marochko.sensors2db;

/**
 * This class reads sensors and writes data to database
 */

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.View;


public class FromSensorsToDB extends IntentService implements SensorEventListener {

    private final String LOG_TAG ="marinfo";

    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Context context;

    MyBinder binder = new MyBinder();

    private boolean transaction_flag = false;
    int count = 0;
    long time = 0;

    ContentValues contentValues = new ContentValues();

    public FromSensorsToDB(Context context){
        super("myService");
        this.context = context;

        Log.d(LOG_TAG, "constructor FromSensorsToDB(Context context), context = " + context);
    }

    public FromSensorsToDB(String name){
        super(name);
        Log.d(LOG_TAG, "FromSensorsToDB(String name)");
    }

    public FromSensorsToDB(){
        super("myService");
        Log.d(LOG_TAG, "FromSensorsToDB()");
    }


    public void startSensors(){
        Log.d(LOG_TAG, "FromSensorsToDB.startSensors()");

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);

        dbHelper = new DBHelper(this);
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

        stopSelf();

    }

    public void readDB(){

        Log.d(LOG_TAG, "FromSensorsToDB.readDB()");

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        Cursor c = db.rawQuery("select * from acceleration", null);
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

    public void clearDB(){

        Log.d(LOG_TAG, "FromSensorsToDB.clearDB");

        db = dbHelper.getWritableDatabase();

        db.delete("acceleration", null, null);

        dbHelper.close();


    }



    public void onAccuracyChanged(Sensor sensor, int accuracy){

        Log.d(LOG_TAG, "FromSensorsToDB.onAccuracyChanged()");

    }


    public void onSensorChanged(SensorEvent event){

        Log.d(LOG_TAG, "FromSensorsToDB.onSensorChanged");

        if(!transaction_flag) return;

        float x,y,z;

        count++;

        x = event.values[0];
        y = event.values[1];
        z = event.values[2];
        time = event.timestamp;

/*
        sensorTextView.setText("x = " + x + "\ny = " + y + "\nz = " + z + "\ncount = " + count
                + "\ntime = " + time);
*/


            contentValues.clear();

            contentValues.put("x", x);
            contentValues.put("y", y);
            contentValues.put("z", z);
            contentValues.put("time", time);

            Log.d(LOG_TAG, contentValues.getAsString("x"));

            db.insert("acceleration", null, contentValues);

            Log.d(LOG_TAG, "record inserted!");
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(LOG_TAG, "FromSensorsToDB.onHandleIntent()");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(LOG_TAG, "FromSensorsToDB.onStartCommand");

//        startSensors();



        return super.onStartCommand(intent, flags, startId);
    }

    class DBHelper extends SQLiteOpenHelper{

        public DBHelper(Context context){
            super(context, "sensorDB", null, 1);
        }

        public void onCreate(SQLiteDatabase db){

            Log.d(LOG_TAG, "FromSensorsToDB.DBHelper.onCreate");

            db.execSQL("create table acceleration(" +
                            "id integer primary key autoincrement," +
                            "x real, y real, z real, time long" +
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


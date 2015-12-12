package name.marochko.sensors2db;

/**
 * This class reads sensors and writes data to database
 */

import android.app.IntentService;
import android.app.Notification;
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
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Timestamp;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;


public class FromSensorsToDB extends IntentService implements SensorEventListener {

    private final String LOG_TAG ="marinfo";

    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private SensorManager mSensorManager;
    private Sensor[] mSensor;
    private Context context;
    private int[] activeSensors;
    private int sensorsDelay;


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


    public void startSensors(Intent intent){

        Log.d(LOG_TAG, "activeSensors == null ? " + Boolean.toString(activeSensors == null));

        int[] activeSensors = new int[intent.getIntExtra("sensors count", 0)];

        for(int i=0; i<activeSensors.length;i++)
            activeSensors[i] = intent.getIntExtra("sensor " + Integer.toString(i), 0);

        mSensor = new Sensor[activeSensors.length];

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        for(int i=0; i<activeSensors.length; i++) {
            mSensor[i] =
                    mSensorManager.getDefaultSensor(activeSensors[i]);
            mSensorManager.registerListener(this, mSensor[i], sensorsDelay);
        }

        db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        transaction_flag = true;

        Log.d(LOG_TAG, "StartSensors thread " + Thread.currentThread());
    }


    public void stopSensors(){

        Log.d(LOG_TAG, "FromSensorsToDB.stopSensors()");

        mSensorManager.unregisterListener(this);

        Log.d(LOG_TAG, "FromSensorsToDB.stopSensors()- unregisterListener ");

        Log.d(LOG_TAG, "StopSensors thread " + Thread.currentThread());

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

        // проверяем доступность SD
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Log.d(LOG_TAG, "SD-карта не доступна: " + Environment.getExternalStorageState());
            return;
        }
        // получаем путь к SD
        File sdPath = Environment.getExternalStorageDirectory();
        // добавляем свой каталог к пути
        sdPath = new File(sdPath.getAbsolutePath() + "/" + DIR_SD);
        // создаем каталог
        sdPath.mkdirs();
        // формируем объект File, который содержит путь к файлу
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

        float x,y,z;


        count++;

        x = event.values[0];
        y = event.values[1];
        z = event.values[2];
        time = event.timestamp;

            contentValues.clear();

            contentValues.put("sens_type", event.sensor.getName());
            contentValues.put("x", x);
            contentValues.put("y", y);
            contentValues.put("z", z);
            contentValues.put("time", time);

            Log.d(LOG_TAG, contentValues.getAsString("x"));

            db.insert("sensors_data", null, contentValues);

            Log.d(LOG_TAG, "record inserted!");
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(LOG_TAG, "FromSensorsToDB.onHandleIntent()");

    }

    protected void startForeground(){

        Log.d(LOG_TAG, "FromSensorsToDB.startForeground()");

        Notification notification = new Notification();

        startForeground(R.mipmap.ic_launcher, notification);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String intentAction = intent.getStringExtra("action");

        Log.d(LOG_TAG, "FromSensorsToDB.onStartCommand");
        Log.d(LOG_TAG, "Intent.action = " + intentAction);

        startForeground();

        switch(intentAction) {

            case "prepare":
                Log.d(LOG_TAG, "Switch - case Prepare");
                dbHelper = new DBHelper(this);
                mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                break;
            case "startSensors":
                Log.d(LOG_TAG, "Switch - case startSensors");
//                Log.d(LOG_TAG, "switch activeSensors " + Boolean.toString(FromSensorsToDB.this.activeSensors == null));

                dbHelper = new DBHelper(this);


                startSensors(intent);
                break;
            case "exportDB":
                break;
            case "stopSensors":
                stopSensors();
                break;
            case "clearDB":
                clearDB();
                break;

//        startSensors();


        }



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

    public void setActiveSensors(int[] activeSensors, int sensorsDelay){



        this.activeSensors = new int[activeSensors.length];
        this.activeSensors = activeSensors.clone();
        this.sensorsDelay = sensorsDelay;

        Log.d(LOG_TAG, "setActiveSensors() activeSensors " + activeSensors.toString());
        Log.d(LOG_TAG, "setActiveSensors() " + this.activeSensors.toString() + Boolean.toString(this.activeSensors == null));
    }

}



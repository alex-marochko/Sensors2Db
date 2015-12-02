package name.marochko.sensors2db;

/**
 * This class reads sensors and writes data to database
 */

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.hardware.SensorManager;


public class FromSensorsToDB extends IntentService{

    private DBHelper dbHelper;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Context context;

    public FromSensorsToDB(Context context){
        super("myservice");
        this.context = context;


    }


    public void startSensors(Context context){

        dbHelper = new DBHelper(context);
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    class DBHelper extends SQLiteOpenHelper{

        public DBHelper(Context context){
            super(context, "sensorDB", null, 1);
        }

        public void onCreate(SQLiteDatabase db){

            db.execSQL("create table acceleration(" +
                            "id integer primary key autoincrement," +
                            "x real, y real, z real, time long" +
                            ");"
            );
        }

        public void onUpgrade(SQLiteDatabase db, int oldversion, int newversion){

        }

    }


}



package com.example.altblesurvey;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

import static com.example.altblesurvey.CameraConfig.SetParameters;
import static com.example.altblesurvey.CameraConfig.getCameraInstance;
import static com.example.altblesurvey.CameraPreview.setCameraDisplayOrientation;
import static com.example.altblesurvey.OutputMediaFile.ImageOutputCallback;

public class MainActivity extends Activity implements BeaconConsumer,SensorEventListener {

    protected static final String TAG = "BEACON";
    private final int BLUETOOTH_SCAN_INTERVAL = 500;
    private final int BETWEEN_SCAN_PERIOD = 0;

    public static Date current;
    public static String time;

    private Camera mCamera;
    private CameraPreview mPreview;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private FrameLayout preview;
    private Camera.PictureCallback mPicture = ImageOutputCallback();
    public static final int MEDIA_TYPE_IMAGE = 1;

    private SensorManager sensorManager;

    private final int CodeLength = 8;
    public static String mCode;
    private final String ALLOWED_CHARACTERS ="0123456789ABCDEFGHIJKLMNPQRSTUVWXYZ";

    Date today = Calendar.getInstance().getTime();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
    String date = formatter.format(today);

    public static final String OUTPUT_DIR = "/sdcard"+ File.separator + "BLE_data";
    private final String csvName = "Alt_CSV_" + date + ".csv";
    private static final int WRITE_PERMISSION_STATIC_CODE_IDENTIFIER = 1;

    private BeaconManager beaconManager;
    private Identifier myBeaconNamespaceId = Identifier.parse("0xedd1ebeac04e5defa017");

    Button mNewID;
    Button mConfirm;
    Button mClear;
    Button mCapture;
    Button mSub;
    Button mUnsub;

    EditText mRoom;
    TextView siteID;
    TextView mBeacons;
    TextView mBLEreadings;
    TextView mImages;
    TextView mTotalRPs;
    TextView mAccX;
    TextView mAccY;
    TextView mAccZ;

    private static int mBLEreadingNumber;
    private static int mImageNumber;
    private static int mRPNumber;
    private static String room = null;
    private static int n;

    private double ax,ay,az;

    private void getCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    private void getDataWritingPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_PERMISSION_STATIC_CODE_IDENTIFIER);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getCameraPermission();
        getDataWritingPermission();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        mRoom = (EditText) findViewById(R.id.mRoom);
        siteID = (TextView) findViewById(R.id.mSiteID);
        mBeacons = (TextView) findViewById(R.id.mBeaconNumber);
        mBLEreadings = (TextView) findViewById(R.id.mReadingNumber);
        mImages = (TextView) findViewById(R.id.mImageNumber);
        mTotalRPs = (TextView) findViewById(R.id.mRPNumber);
        mAccX = (TextView) findViewById(R.id.AccX);
        mAccY = (TextView) findViewById(R.id.AccY);
        mAccZ = (TextView) findViewById(R.id.AccZ);

        mNewID = (Button) findViewById(R.id.mNewID);
        mConfirm = (Button) findViewById(R.id.mConfirm);
        mClear = (Button) findViewById(R.id.mClear);
        mSub = (Button) findViewById(R.id.mSubscribe);
        mUnsub = (Button) findViewById(R.id.mUnsubscribe);
        mCapture = (Button) findViewById(R.id.mCapture);

        mCamera = getCameraInstance();
        SetParameters(mCamera);

        mPreview = new CameraPreview(this, mCamera);
        preview = findViewById(R.id.mCameraFrame);
        preview.addView(mPreview);
        setCameraDisplayOrientation(this, 0, mCamera);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        beaconManager.setForegroundScanPeriod(BLUETOOTH_SCAN_INTERVAL);
        beaconManager.setForegroundBetweenScanPeriod(BETWEEN_SCAN_PERIOD);
        beaconManager.bind(this);

        mUnsub.setEnabled(false);
        mCapture.setEnabled(false);

        mAccX.setText("" + 0);
        mAccY.setText("" + 0);
        mAccZ.setText("" + 0);

        mBeacons.setText("" + 0);
        mBLEreadings.setText("" + 0);
        mImages.setText("" + 0);
        mTotalRPs.setText("" + 0);

        mBLEreadingNumber = 0;
        mImageNumber = 0;
        mRPNumber = 0;
        n = 0;

    }

    private String mIDgenerator(final int sizeOfRandomString)
    {
        final Random random=new Random();
        final StringBuilder sb=new StringBuilder(sizeOfRandomString);
        for(int i=0;i<sizeOfRandomString;++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    public void NewSiteID(View view) {
        mRPNumber = mRPNumber + 1;
        mCode = mIDgenerator(CodeLength);
        siteID.setText(mCode);
        mTotalRPs.setText("" + mRPNumber);
        n = 0;
        mBLEreadingNumber = 0;
        mBLEreadings.setText("" + n);
    }

    public void Confirm(View view) {
        mConfirm.setEnabled(false);
        mClear.setEnabled(true);
        mRoom.setEnabled(false);
        room = mRoom.getText().toString();
    }

    public void Clear(View view) {
        mConfirm.setEnabled(true);
        mClear.setEnabled(true);
        mRoom.setEnabled(true);
    }

    public void Subcribe(View view) {
        if (mCode != null) {
                mNewID.setEnabled(false);
                mClear.setEnabled(false);
                mConfirm.setEnabled(false);
                mSub.setEnabled(false);
                mUnsub.setEnabled(true);
                mCapture.setEnabled(true);
                try {
                    beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", myBeaconNamespaceId, null, null));
                } catch (RemoteException e) { }
        } else {
            Toast.makeText(this, "Path ID is empty! Please generate New!", Toast.LENGTH_LONG).show();
        }
    }

    public void Unsubscribe(View view) {
        mNewID.setEnabled(true);
        mClear.setEnabled(true);
        mConfirm.setEnabled(true);
        mSub.setEnabled(true);
        mUnsub.setEnabled(false);
        mCapture.setEnabled(false);
        try {
            beaconManager.stopRangingBeaconsInRegion(new Region("myRangingUniqueId", myBeaconNamespaceId, null, null));
        } catch (RemoteException e) {        }
        mBeacons.setText("" + 0);
    }

    public void Capture(View view) {
        if (mCode != null) {
            mCamera.takePicture(null, null, mPicture);
            mImageNumber += 1;
            mImages.setText("" + mImageNumber);

            Toast.makeText(this,
                    "Image has been saved successfully!",
                    Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(this, "Site ID is empty! Please generate New!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                mBeacons.setText("" + beacons.size());

                if (beacons.size() > 0) {
                    java.util.Iterator iterator = beacons.iterator();
                    while (iterator.hasNext()) {

                        Beacon beacon = (Beacon) iterator.next();

                        String beacon_id = beacon.getId2().toString();
                        String rssi = "" + beacon.getRssi();
                        String avg_rssi =  String.format("%.4f", beacon.getRunningAverageRssi());
                        String distance =  String.format("%.4f", beacon.getDistance());

                        BLE_writer(beacon_id, rssi, avg_rssi, distance);

//                        Log.i(TAG, "X: " + ax + " Y: " + ay + " Z: " + az);
//                        Log.i(TAG, "Beacon detected! Name Space: " + beacon.getId1() + " Beacon ID: " + beacon_id + " rssi: " + rssi);

                        mBLEreadingNumber += 1;
                        n = mBLEreadingNumber / beacons.size();
                        mBLEreadings.setText("" + n);

                    }
                }
            }
        });
    }

    public void BLE_writer(String beacon, String beacon_rssi, String avg_rssi, String distance) {
        current = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
        time = formatter.format(current);

        File folder = new File(OUTPUT_DIR);

        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {

            File test = new File(folder, csvName);

            try {
                FileWriter writer = new FileWriter(test, true);

                writer.append("" + mCode + "," + room + "," + time + "," + beacon + "," + beacon_rssi + "," + avg_rssi + "," + distance + "," + ax + "," + ay + "," + az + "\n");

                writer.flush();
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            // Do something else on failure
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            ax=event.values[0];
            ay=event.values[1];
            az=event.values[2];

            mAccX.setText(String.format("%.4f", ax));
            mAccY.setText(String.format("%.4f", ay));
            mAccZ.setText(String.format("%.4f", az));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
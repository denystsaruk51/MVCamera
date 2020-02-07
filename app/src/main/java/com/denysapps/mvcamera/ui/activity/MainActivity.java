package com.denysapps.mvcamera.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.denysapps.mvcamera.GPSService;
import com.denysapps.mvcamera.R;
import com.denysapps.mvcamera.ui.view.CameraView;
import com.denysapps.mvcamera.utility.CommonUtil;
import com.denysapps.mvcamera.utility.ResourceUtil;
import com.github.siyamed.shapeimageview.RoundedImageView;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static com.denysapps.mvcamera.GPSService.ACCURACY;
import static com.denysapps.mvcamera.GPSService.IDENTIFIER;
import static com.denysapps.mvcamera.GPSService.LAT;
import static com.denysapps.mvcamera.GPSService.LNG;
import static com.denysapps.mvcamera.utility.CommonUtil.TYPE_ALL_PERMISSION;

public class MainActivity extends BaseActivity implements CameraView.CustomCameraCallback {
    public static final int REQUEST_PERMISSION = 1;
    public static String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static int REQUEST_LOCATION = 1001;
    public static final long MINIMUM_TIME_BETWEEN_UPDATES = 5000;
    public static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 0;

    @BindView(R.id.camera_preview)
    LinearLayout camera_preview;

    @BindView(R.id.btn_close)
    ImageView btn_close;
    @BindView(R.id.btn_flash)
    ImageView btn_flash;
    @BindView(R.id.btn_focus)
    ImageView btn_focus;

    @BindView(R.id.btn_capture)
    ImageView btn_capture;

    @BindView(R.id.txt_gps_state)
    TextView txt_gps_state;
    @BindView(R.id.txt_gps_coordinate)
    TextView txt_gps_coordinate;

    @BindView(R.id.img_pre_photo)
    RoundedImageView img_pre_photo;

    private static MainActivity instance;
    private CameraView mCameraView;
    private boolean isPressedCaptureButton = false;
    private Handler handler;

    private LocationManager mLocationManager;
    private double mLatitude = 0;
    private double mLongitude = 0;
    private float accuracy = 0;
    private String lastPicturePath = "";
    private boolean needRequireLocation = true;
    private LocationSettingsRequest locationSettingsRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        instance = this;

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(1000)
                .setFastestInterval(500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }
    @Override
    public void onBackPressed(){
        onBack();
    }
    @OnClick(R.id.btn_close)
    public void onBack(){
        finish();
    }
    /*
     capture image from camera
    */
    @OnClick(R.id.btn_capture)
    public void onCapture(){
        if(mLatitude == 0 && mLongitude == 0){
            return;
        }

        if(!isPressedCaptureButton)
            captureScreen();
    }
    /*
     set flash sate
    */
    @OnClick(R.id.btn_flash)
    public void onChangeFlash(){
        if(isPressedCaptureButton)
            return;

        if (mCameraView.getFlash().equals(FLASH_MODE_TORCH)) {
            btn_flash.setImageResource(R.drawable.ico_camera_flash_no);
            mCameraView.setFlash(FLASH_MODE_OFF);
        } else {
            btn_flash.setImageResource(R.drawable.ico_camera_flash);
            mCameraView.setFlash(FLASH_MODE_TORCH);
        }
    }
    /*
     set focus
    */
    @OnClick(R.id.btn_focus)
    public void onClickedFocus(){
        if(isPressedCaptureButton)
            return;

        if(mCameraView != null)
            mCameraView.setFocus();
    }
    /*
     show latest captured image.
    */
    @OnClick(R.id.img_pre_photo)
    public void onClickPrePhoto(){
        if(!TextUtils.isEmpty(lastPicturePath)){
            ImageViewActivity.createImageViewActivity(this, lastPicturePath);
        }
    }

    @Override
    public void onStop(){
        super.onStop();

        needRequireLocation = true;
    }
    @Override
    public void onResume() {
        super.onResume();

        initPrePicture();

        camera_preview.setVisibility(View.VISIBLE);

        if (CommonUtil.verifyPermissions(TYPE_ALL_PERMISSION, this)) {
            btn_capture.setEnabled(true);
            mCameraView = new CameraView(this, this);
            camera_preview.removeAllViews();
            camera_preview.addView(mCameraView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
            enableControls(true);
            btn_flash.setImageResource(R.drawable.ico_camera_flash_no);

            mCameraView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mCameraView.focusOnTouch(event);
                    }
                    if(event.getPointerCount() == 2){
                        if (event.getAction() == MotionEvent.ACTION_POINTER_DOWN) {
                            mCameraView.mDist = mCameraView.getFingerSpacing(event);
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE && mCameraView.isZoomSupported()) {
                            mCameraView.handleZoom(event);
                        }
                    }

                    return true;
                }
            });
        } else {
            btn_capture.setEnabled(false);
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION
            );
        }

        checkLocation();
    }
    /*
     get latest captured image.
    */
    public String getRecentFile() {
        String ret = null;
        File parentDir = new File(ResourceUtil.getResourceDirectory());
        long lastMod = Long.MIN_VALUE;
        File[] files = parentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > lastMod) {
                    lastMod = file.lastModified();
                    ret = file.getPath();
                }
            }
        }

        return ret;
    }
    @Override
    public void onPause() {
        super.onPause();
        if (mCameraView !=null)
            mCameraView.cancel();

        try {
            unregisterReceiver(rec);
        } catch (Exception e){}

        Intent intent = new Intent(this, GPSService.class);
        stopService(intent);
    }

    /*
     control all buttons state when user capture image from camera
    */
    public void enableControls(boolean bEnable) {
        btn_capture.setClickable(bEnable);
        btn_flash.setClickable(bEnable);
        btn_focus.setClickable(bEnable);
    }

    /*
     capture image from camera.
    */

    @SuppressLint("SimpleDateFormat")
    private void captureScreen() {
        if(accuracy > 100 || (mLatitude == 0 && mLongitude == 0)){
            return;
        }

        Toast.makeText(this, "Photo is being taken.", Toast.LENGTH_LONG).show();

        isPressedCaptureButton = true;

        enableControls(false);

        ResourceUtil.setPhotoExtension("jpg");
        String fileName = ResourceUtil.getCaptureImageFilePath(this);
        File tempFile = new File(fileName);

        if (tempFile.exists()) {
            tempFile.delete();
        }

        try {
            tempFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long l = tempFile.length();
        mCameraView.takePicture(fileName);
    }

    /*
     getting image file from captured image.
    */
    @Override
    public void onPictureTaken(String strFileName) {
        int cameraID = mCameraView.getCamera();
        int rotation = ResourceUtil.getRotationAngle(this, cameraID);
        Bitmap bm = null;
        try {
            bm = BitmapFactory.decodeFile(strFileName);
        }
        catch(Exception e) {
            e.printStackTrace();
            bm = null;
        }

        if (bm == null) {
            mCameraView.startPreview();
            isPressedCaptureButton = false;
            return;
        }

        Bitmap rotatedBitmap = null;
        if (rotation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            try {
                rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            }
            catch(Exception e) {
                e.printStackTrace();
                rotatedBitmap = null;
            }
        } else {
            rotatedBitmap = bm;
        }

        if (rotatedBitmap == null) {
            mCameraView.startPreview();
            bm.recycle();
            isPressedCaptureButton = false;
            return;
        }
        if (bm != rotatedBitmap)
            bm.recycle();

        File file = new File(strFileName);
        if (file.exists()) {
            try {
                file.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ResourceUtil.SaveToBitmap(rotatedBitmap, strFileName, mLatitude, mLongitude, txt_gps_state.getText().toString(), accuracy);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + strFileName)));
        img_pre_photo.setVisibility(View.VISIBLE);
        img_pre_photo.setImageBitmap(rotatedBitmap);
        lastPicturePath = strFileName;
        Toast.makeText(this, "Photo taking is completed. Photo is saved " + strFileName, Toast.LENGTH_LONG).show();

        showProgressDialog();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                enableControls(true);
                mCameraView.startPreview();
                isPressedCaptureButton = false;
                hidProgressDialog();
            }
        }, 1000);
    }

    /*
     set GPS state.
    */
    private void setGPSState(){
        if( accuracy > 100 || ( mLatitude == 0 && mLongitude == 0 ) ) {
            btn_capture.setImageResource(R.drawable.bg_capture_red);

            txt_gps_state.setText("GPS: Poor");
            txt_gps_coordinate.setText("");
        } else if(accuracy > 30 && (mLatitude != 0 && mLongitude != 0)){
            btn_capture.setImageResource(R.drawable.bg_capture_yellow);
            txt_gps_state.setText("GPS:weak - " + String.format("%3.2f", accuracy));
            txt_gps_coordinate.setText(String.format("%3.6f", mLatitude) + " " + String.format("%3.6f", mLongitude));
        } else {
            btn_capture.setImageResource(R.drawable.bg_capture_green);
            txt_gps_state.setText("GPS:good - " + String.format("%3.2f", accuracy));
            txt_gps_coordinate.setText(String.format("%3.6f", mLatitude) + " " + String.format("%3.6f", mLongitude));
        }
    }

    /*
     open location setting dialog
    */
    public void openSettings() {

        needRequireLocation = false;


        LocationServices
                .getSettingsClient(this)
                .checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (e instanceof ResolvableApiException) {
                            // Location settings are NOT satisfied,  but this can be fixed  by showing the user a dialog.
                            try {
                                // Show the dialog by calling startResolutionForResult(),  and check the result in onActivityResult().
                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                resolvable.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
                            } catch (IntentSender.SendIntentException sendEx) {
                                // Ignore the error.
                            }
                        }
                    }
                });
    }

    /*
     check location service
    */
    private void checkLocation() {
        if (isLocationEnabled(this))
            getlocation();
        else {
            if(needRequireLocation) {
                openSettings();
            }

            accuracy = 9999F;
            mLatitude = 0;
            mLongitude = 0;

            setGPSState();
        }
    }
    //Check if internet service is available and get address string either using Geocoder
    //Or if internet is not available getAddressString(LatLng obj) will return co-ordinates
    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }
    /*
     get current location.
    */
    @SuppressLint("MissingPermission")
    public double[] getlocation() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!CommonUtil.verifyPermissions(CommonUtil.TYPE_LOCATION_PERMISSION, this)) {
                setGPSState();
                double[] gps = new double[2];
                gps[0] = 0;
                gps[1] = 0;

                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
                }, 10);

                return gps;
            }
        }

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        boolean networkEnabled = false;
        try {
            networkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ex) {
            Log.e("fail to request", ex.getLocalizedMessage());
        } catch (IllegalArgumentException ex) {
            Log.e("provider does", ex.getMessage());
        }
        boolean gpsEnabled = false;
        try {
            gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (SecurityException ex) {
            Log.e("fail to reques", ex.getLocalizedMessage());
        } catch (IllegalArgumentException ex) {
            Log.e("provider does", ex.getMessage());
        }

        if (!gpsEnabled && !networkEnabled) {
            setGPSState();
        }
        if (!gpsEnabled) {
            setGPSState();
        }

        Location location = null;
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) { }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override
            public void onProviderEnabled(String provider) { }
            @Override
            public void onProviderDisabled(String provider) { }
        };

        if (networkEnabled) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    MINIMUM_TIME_BETWEEN_UPDATES,
                    MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
            location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            accuracy = 9999F;
            if (location != null) {
                if(location.hasAccuracy()) {
                    accuracy = location.getAccuracy();
                }
                mLatitude= location.getLatitude();
                mLongitude = location.getLongitude();
            }
        }

        if (gpsEnabled) {
            if (location == null) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MINIMUM_TIME_BETWEEN_UPDATES,
                        MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
                location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                accuracy = 9999F;

                if (location != null) {
                    if(location.hasAccuracy()) {
                        accuracy = location.getAccuracy();
                    }
                    mLatitude = location.getLatitude();
                    mLongitude = location.getLongitude();
                }
            }
        }
        double[] gps = new double[2];
        gps[0] = mLatitude;
        gps[1] = mLongitude;

        registerReceiver(rec, new IntentFilter(IDENTIFIER));
        Intent intent = new Intent(this, GPSService.class);
        startService(intent);

        setGPSState();

        return gps;
    }

    /*
     set latest image
    */
    private void initPrePicture(){
        handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String lastFilePath = getRecentFile();
                        if (!TextUtils.isEmpty(lastFilePath)){
                            File file = new File(lastFilePath);
                            if (file.exists())
                            {
                                lastPicturePath = lastFilePath;
                                Bitmap bmp = ResourceUtil.decodeSampledBitmapFromFile(lastFilePath, 500);
                                if (bmp != null) {
                                    img_pre_photo.setVisibility(View.VISIBLE);
                                    img_pre_photo.setImageBitmap(bmp);
                                }
                            } else {
                                img_pre_photo.setVisibility(View.GONE);
                            }
                        } else {
                            img_pre_photo.setVisibility(View.GONE);
                        }
                    }
                });
                Looper.loop();
            };
        }.start();
    }

    /*
     get GPS state
    */
    private BroadcastReceiver rec = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            mLatitude = intent.getDoubleExtra(LAT,0.0);
            mLongitude = intent.getDoubleExtra(LNG,0.0);
            accuracy = intent.getFloatExtra(ACCURACY,9999F);

            setGPSState();
        }
    };
}

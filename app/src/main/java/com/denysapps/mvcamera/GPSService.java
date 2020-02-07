package com.denysapps.mvcamera;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GPSService extends Service {
    private static final String TAG = GPSService.class.getSimpleName();
    public static final String IDENTIFIER = "Location";
    public static final String LAT = "latData";
    public static final String LNG = "lngData";
    public static final String CDT = "timeData";
    public static final String ACCURACY = "accuracy";
    private static int L_INT;
    private static final float L_DIST = 0.3F;
    private LocationManager lm = null;
    private Location starterLoc;

    /*
     location event listener.
    */
    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            //Define locationListener object passing in network provider as a string
            mLastLocation = new Location(provider);
            Log.i(TAG, "LocationListener " + provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            //Define date format
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
            String cdt = sdf.format(new Date());

            //get latitude & longitude values from location object;
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            //Create LatLng Object (To Be Used to Create Google Maps Marker)
            Location lCurr = new Location(location);

            if (starterLoc == null) {
                starterLoc = new Location(location);
            }

            float accuracy = 9999F;
            if(location.hasAccuracy()) {
                accuracy = location.getAccuracy();
            }

            //Create an identified intent to broadcast updated location to fragment object
            Intent intent = new Intent(IDENTIFIER); //IDENTIFIER is a string to identify this intent
            //Add values of current location to intent
            intent.putExtra(LAT, lat); //Latitude of current location
            intent.putExtra(LNG, lng); //Longitude of current location
            intent.putExtra(CDT, cdt); //Timestamp i.e. When location was recorded/changed
            intent.putExtra(ACCURACY, accuracy);

            //Broadcast this data to be received by Broadcast Receiver in HomeFragment class.
            sendBroadcast(intent);

            Log.i(TAG, "onLocationChanged: " + lCurr.toString());
            mLastLocation.set(location);

            Log.i(TAG, "Sending Location Data To Update UI");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.w(TAG, "onProviderDisabled: " + provider);
        }
        @Override
        public void onProviderEnabled(String provider) {
            Log.i(TAG, "onProviderEnabled: " + provider);
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
      create GPS service
    */
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        L_INT = 500;
        initializeLocationManager();

        try {
            //Request location updates from LocationManager
            lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, L_INT, L_DIST,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.e(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, L_INT, L_DIST,
                    mLocationListeners[0]);

        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    /*
      destroy GPS service
    */
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        if (lm != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    lm.removeUpdates(mLocationListeners[i]);
                } catch (SecurityException ex) {
                    Log.i(TAG, "fail to remove location listeners, ignore", ex);
                }
            }
        }
    }

    /*
      initialize location manager
    */
    private void initializeLocationManager() {
        Log.i(TAG, "initializeLocationManager");
        if (lm == null) {
            lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    /*
      start GPS service
    */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand ");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }
}

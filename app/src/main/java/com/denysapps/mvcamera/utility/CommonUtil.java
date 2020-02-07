package com.denysapps.mvcamera.utility;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

public class CommonUtil {
    public static int TYPE_ALL_PERMISSION = 0;
    public static int TYPE_CAMERA_PERMISSION =1;
    public static int TYPE_LOCATION_PERMISSION =2;
    public static int TYPE_STORAGE_PERMISSION =3;

    /*
        Check App Permission
    */
    public static boolean verifyPermissions(final int type, final Activity activity) {
        // Check if we have write permission
        int permission0 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission1 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        int permission2 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
        int permission3 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (type == TYPE_ALL_PERMISSION) {
            if (permission0 != PackageManager.PERMISSION_GRANTED
                    || permission1 != PackageManager.PERMISSION_GRANTED
                    || permission2 != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        } else if (type == TYPE_CAMERA_PERMISSION) {
            if (permission1 != PackageManager.PERMISSION_GRANTED
                || permission0 != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        } else if (type == TYPE_STORAGE_PERMISSION) {
            if (permission0 != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        else if (type == TYPE_LOCATION_PERMISSION) {
            if (permission2 != PackageManager.PERMISSION_GRANTED
                || permission3 != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}

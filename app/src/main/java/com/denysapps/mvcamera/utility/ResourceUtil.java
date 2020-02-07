package com.denysapps.mvcamera.utility;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Environment;
import android.support.media.ExifInterface;
import android.text.TextUtils;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ResourceUtil {
    public static String RES_DIRECTORY = Environment.getExternalStorageDirectory() + "/MVCamera/";

    /*
       Get Bitmap date from image file with required width
     */
    public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        BitmapFactory.decodeFile(path, options);
        int reqHeight = reqWidth * options.outHeight / options.outWidth;
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    /*
       Calculate sample size from original image with required width
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (width <= reqWidth && height <= reqHeight)
            return 1;

        float widthRatio = (float)width / reqWidth;
        float heightRatio = (float)height / reqHeight;
        float maxRatio = Math.max(widthRatio, heightRatio);
        inSampleSize = (int)(maxRatio + 0.5);
        return inSampleSize;
    }

    /*
       get App default directory.
     */
    public static String getResourceDirectory() {
        String tempDirPath = RES_DIRECTORY;
        File tempDir = new File(tempDirPath);
        if (!tempDir.exists())
            tempDir.mkdirs();

        return tempDirPath;
    }
    private static String mPhotoFileExtension = "jpg";

    /*
       set image file's extension.
     */
    public static void setPhotoExtension(String fileExtension) {
        mPhotoFileExtension = fileExtension;
    }

    /*
       get new capture image file path.
     */
    public static String getCaptureImageFilePath(Context context) {
        Date date= new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateStr = dateFormat.format(date);
        return getResourceDirectory() + "GPSCamera_" + dateStr + "." + mPhotoFileExtension;
    }
    /*
       get rotation value of image.
     */
    public static int getRotationAngle(Activity mContext, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = mContext.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /*
       save image to disk.
     */
    public static void SaveToBitmap(Bitmap bitmap, String outputFilePath, double latitude, double longitude, String gpsState, float accuracy ) {
        if (bitmap == null || TextUtils.isEmpty(outputFilePath))
            return;

        File outputFile = new File(outputFilePath);
        if (outputFile.exists())
            outputFile.delete();

        // write to bitmap
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Date now = new Date();

        try {
            ExifInterface exif = new ExifInterface(outputFilePath);
            exif.setLatLong(latitude, longitude);
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_FLIP_HORIZONTAL));
            exif.setAttribute(ExifInterface.TAG_CAMARA_OWNER_NAME, "MVCamera");

            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, now.toString());

            exif.setAttribute(ExifInterface.TAG_X_RESOLUTION, "72/1");
            exif.setAttribute(ExifInterface.TAG_Y_RESOLUTION, "72/1");

            exif.setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, String.valueOf(ExifInterface.RESOLUTION_UNIT_INCHES));

            exif.setAttribute(ExifInterface.TAG_GPS_VERSION_ID, "2.2.0.0");
            exif.setAttribute(ExifInterface.TAG_GPS_STATUS, String.valueOf(accuracy));
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd");
            String dateStr = dateFormat.format(now);
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, dateStr);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            String timeStr = timeFormat.format(now);
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, timeStr);
            exif.setAttribute(ExifInterface.TAG_GPS_MEASURE_MODE, String.valueOf(accuracy));
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, String.valueOf(ExifInterface.ALTITUDE_BELOW_SEA_LEVEL));
            exif.setAltitude(ExifInterface.ALTITUDE_ABOVE_SEA_LEVEL);
            exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, "fused");
            exif.setAttribute(ExifInterface.TAG_APERTURE_VALUE, "1.8");
            exif.setAttribute(ExifInterface.TAG_EXIF_VERSION, "0220");

            exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, "1/96");
            exif.setAttribute(ExifInterface.TAG_F_NUMBER, "1.8");
            exif.setAttribute(ExifInterface.TAG_EXPOSURE_PROGRAM, "Program AE");
            exif.setAttribute(ExifInterface.TAG_RW2_ISO, "52");
            exif.setAttribute(ExifInterface.TAG_Y_CB_CR_POSITIONING, String.valueOf(ExifInterface.Y_CB_CR_POSITIONING_CENTERED));
            exif.setAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION, String.valueOf(ExifInterface.Y_CB_CR_POSITIONING_CENTERED));
            exif.setAttribute(ExifInterface.TAG_BRIGHTNESS_VALUE, "4.67");
            exif.setAttribute(ExifInterface.TAG_EXPOSURE_INDEX, "0");
            exif.setAttribute(ExifInterface.TAG_METERING_MODE, String.valueOf(ExifInterface.METERING_MODE_CENTER_WEIGHT_AVERAGE));
            exif.setAttribute(ExifInterface.TAG_COLOR_SPACE, String.valueOf(ExifInterface.COLOR_SPACE_S_RGB));
            exif.setAttribute(ExifInterface.TAG_INTEROPERABILITY_INDEX, String.valueOf(ExifInterface.COLOR_SPACE_S_RGB));
            exif.setAttribute(ExifInterface.TAG_SENSING_METHOD, "One-chip color area");
            exif.setAttribute(ExifInterface.TAG_SCENE_TYPE, "Directly photographed");
            exif.setAttribute(ExifInterface.TAG_CUSTOM_RENDERED, "Custom");
            exif.setAttribute(ExifInterface.TAG_EXPOSURE_MODE, "Auto");
            exif.setAttribute(ExifInterface.TAG_WHITE_BALANCE, "Auto");
            exif.setAttribute(ExifInterface.TAG_DIGITAL_ZOOM_RATIO, "0");
            exif.setAttribute(ExifInterface.TAG_SCENE_CAPTURE_TYPE, "Standard");
            exif.setAttribute(ExifInterface.TAG_CONTRAST, "normal");
            exif.setAttribute(ExifInterface.TAG_SATURATION, "normal");
            exif.setAttribute(ExifInterface.TAG_SHARPNESS, "normal");
            exif.setAttribute(ExifInterface.TAG_FLASH, "off, Did not fire");
            exif.setAttribute(ExifInterface.TAG_SUBJECT_DISTANCE_RANGE, "Distant");
            exif.setAttribute(ExifInterface.TAG_COMPRESSION, "JPEG (old-style)");
            exif.setAttribute(ExifInterface.TAG_COLOR_SPACE, "RGB");
            exif.setAttribute(ExifInterface.TAG_WHITE_POINT, "0.95045 1 1.08905");
            exif.setAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE, "1/96");
            exif.setAttribute(ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH, "(Binary data 8573 bytes, use -b option to extract)");
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "4.4 mm (35 mm equivalent: 27.0 mm)");
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, "27 mm");
            exif.setAttribute(ExifInterface.TAG_LIGHT_SOURCE, "9.2");

//            exif.setAttribute("GPS Date/Time", dateStr + " " + timeStr);
//            exif.setAttribute("GPS Dilution Of Precision", "18.736");
//            exif.setAttribute("Create Date", now.toString());
//            exif.setAttribute("Modify Date", now.toString());
//            exif.setAttribute("Circle Of Confusion", "0.005 mm");
//            exif.setAttribute("Depth Of Field", "inf (2.22 m - inf)");
//            exif.setAttribute("Field Of View", "67.4 deg");
//            exif.setAttribute("Hyperfocal Distance", "2.22 m");

            exif.saveAttributes();
        } catch (Exception e){}
    }
}

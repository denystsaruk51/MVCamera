package com.denysapps.mvcamera.ui.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraView extends SurfaceView implements Camera.PreviewCallback, SurfaceHolder.Callback {
	private static final String TAG = "CameraView";

	private Context mContext;
	private CustomCameraCallback mCallback = null;
	private Camera mCamera;
	private SurfaceHolder mSurfaceHolder;
	private String mPictureFileName;
	private boolean mPreviewing = false;
	private String mFlashMode = Camera.Parameters.FLASH_MODE_ON;
	private int mOpenCamera = Camera.CameraInfo.CAMERA_FACING_BACK;

	Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {

		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(TAG, "Saving a photo to file");
		}
	};

	/*
	 create Camera view
	*/
	public CameraView(Context context, CustomCameraCallback callback) {
		super(context);
		Log.d(TAG, "CameraView construct");

		mOpenCamera = CameraInfo.CAMERA_FACING_BACK;

		mContext = context;
		mFlashMode = Parameters.FLASH_MODE_OFF;
		mSurfaceHolder = getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mCallback = callback;
	}

    /*
     set Camera params when camera surface is created
    */
	@Override
	public void surfaceCreated(SurfaceHolder paramSurfaceHolder) {
		Log.d(TAG, "surfaceCreated");

		mCamera = Camera.open(mOpenCamera);
		setCameraDisplayOrientation(mOpenCamera, mCamera);

		mbAutoFocusSupport = false;
		PackageManager pm = mContext.getPackageManager();
		if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
			mbAutoFocusSupport = true;
		}

		Parameters params = mCamera.getParameters();
		if (mOpenCamera == Camera.CameraInfo.CAMERA_FACING_BACK)
			params.setFlashMode(mFlashMode);

		mCamera.setParameters(params);
	}

    /*
     set Camera params when camera surface is destroyed
    */
	@Override
	public void surfaceDestroyed(SurfaceHolder paramSurfaceHolder) {
		Log.d(TAG, "surfaceDestroyed");

		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		mPreviewing = false;
	}

    /*
     set Camera sate when camera surface is changed
    */
	@Override
	public void surfaceChanged(SurfaceHolder paramSurfaceHolder, int format, int width, int height) {
		Log.d(TAG, "surfaceChanged");

		if (mPreviewing) {
			mCamera.stopPreview();
			mPreviewing = false;
		}

		mIsTakingPhoto = false;
		mbAutoFocusSuccess = false;
		if (mCamera != null)
			;
		try {
			mCamera.setPreviewDisplay(mSurfaceHolder);
			mCamera.startPreview();
			mCamera.setPreviewCallback(this);
			mPreviewing = true;
		} catch (IOException localIOException) {
			localIOException.printStackTrace();
		}
	}

    /*
     set Camera flash
    */
	public void setFlash(String inFlash) {
		mFlashMode = inFlash;

		Parameters params = mCamera.getParameters();
		if (mOpenCamera == Camera.CameraInfo.CAMERA_FACING_BACK)
			params.setFlashMode(mFlashMode);

		mCamera.setParameters(params);
	}

    /*
     get Camera flash
    */

	public String getFlash() {
		return mFlashMode;
	}

    /*
     set Camera orientation
    */

	private void setCameraDisplayOrientation(int cameraID2, Camera camera2) {
		if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
			mCamera.setDisplayOrientation(90);
		}
	}

    /*
     set Camera state when camera view is destroyed
    */

	public void cancel() {
		Log.d(TAG, "cancel");

		if (mCamera != null) {
			try {
				mCamera.cancelAutoFocus();

				mCamera.setPreviewCallback(null);
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
				mPreviewing = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

    /*
     get image from Camera with image file path
    */
	public void takePicture(String fileName) {
		Log.d(TAG, "takePicture");

		mPictureFileName = fileName;

		try {
			mIsTakingPhoto = true;
			if (mbAutoFocusSupport) {
				mCamera.autoFocus(mAutoFocusCallback);
			} else {
				mCamera.takePicture(null, null, jpegCallback);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    /*
     get Camera
    */

	public int getCamera() {
		return mOpenCamera;
	}

    /*
     start camera preview
    */
	public void startPreview() {
		if (!mPreviewing) {
			mCamera.startPreview();
			mCamera.setPreviewCallback(this);
			mPreviewing = true;
		}
	}

    /*
     start camera focus
    */
	public void setFocus() {
		if(mCamera != null) {
//			mCamera.autoFocus(null);
			int width = getWidth();
			int height = getHeight();
			mCamera.cancelAutoFocus();
			Rect focusRect = calculateTapArea(width/2, height/2);
			Parameters parameters = mCamera.getParameters();
			parameters.setFocusMode(Parameters.FOCUS_MODE_MACRO);

			if (parameters.getMaxNumFocusAreas() > 0) {
				List<Camera.Area> mylist = new ArrayList<Camera.Area>();
				mylist.add(new Camera.Area(focusRect, 1000));
				parameters.setFocusAreas(mylist);
			}

			mCamera.setParameters(parameters);
			mCamera.autoFocus(new Camera.AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					camera.cancelAutoFocus();
					Parameters params = camera.getParameters();
					if (!params.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
						params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
						camera.setParameters(params);
					}
				}
			});
		}
	}

	private boolean mIsTakingPhoto = false;
	private boolean mbAutoFocusSuccess = false;
	private boolean mbAutoFocusSupport = false;

	private AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback() {

		/**
		 * function called when auto focus is completed
		 * @param success tells if autofocus where successfully acomplished
		 * @param camera android camera object
		 */
		public void onAutoFocus(boolean success, Camera camera) {
			mbAutoFocusSuccess = true;
		}
	};

    /*
     processing camera preview frame is changed
    */
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (mIsTakingPhoto && mbAutoFocusSuccess) {
			mIsTakingPhoto = false;

			switch (camera.getParameters().getPreviewFormat()) {
			case ImageFormat.NV21:
			case ImageFormat.NV16:
				boolean success = false;
				try {
					mCamera.stopPreview();

					Size previewSize = camera.getParameters().getPreviewSize();
					Rect clipRect = new Rect(0, 0, previewSize.width, previewSize.height);

					YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					yuvImage.compressToJpeg(clipRect, 80, baos);

					// Save to file.
					FileOutputStream fos = new FileOutputStream(mPictureFileName);
					fos.write(baos.toByteArray());
					fos.flush();
					fos.close();

					success = true;
					baos.close();

					if (mCallback != null) {
						mPreviewing = false;
						mCallback.onPictureTaken(mPictureFileName);
						mCamera.setPreviewCallback(null);
					}
				} catch (Exception e) {
					Log.e("onPictureTaken()", "Error converting bitmap");
					success = false;
				}

				// setting result of activity and finishing
				if (success) {
				} else {
					Log.d("CAMERA", "Photo Damaged!!!!");
				}
				break;
			default:
				break;
			}
		}
	}

	public interface CustomCameraCallback {
		public void onPictureTaken(String strFileName);
	}

    /*
     set focus area when user click camera preview.
    */

	public void focusOnTouch(MotionEvent event){
		if(mCamera != null) {
			mCamera.cancelAutoFocus();
			Rect focusRect = calculateTapArea(event.getX(), event.getY());
			Parameters parameters = mCamera.getParameters();
			parameters.setFocusMode(Parameters.FOCUS_MODE_MACRO);

			if (parameters.getMaxNumFocusAreas() > 0) {
				List<Camera.Area> mylist = new ArrayList<Camera.Area>();
				mylist.add(new Camera.Area(focusRect, 1000));
				parameters.setFocusAreas(mylist);
			}

			try {
				mCamera.setParameters(parameters);
				mCamera.autoFocus(new Camera.AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean success, Camera camera) {
						camera.cancelAutoFocus();
						Parameters params = camera.getParameters();
						if (!params.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
							params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
							camera.setParameters(params);
						}
					}
				});
			} catch (Exception e) { }
		}
	}

    /*
     calculate focus area
    */

	private Rect calculateTapArea(float x, float y) {
		int left = clamp(Float.valueOf((x / getWidth()) * 2000 - 1000).intValue(), 1);
		int top = clamp(Float.valueOf((y / getHeight()) * 2000 - 1000).intValue(), 1);
		return new Rect(left, top, left + 1, top + 1);
	}
	private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
		int result;
		if (Math.abs(touchCoordinateInCameraReper)+focusAreaSize/2>1000){
			if (touchCoordinateInCameraReper>0){
				result = 1000 - focusAreaSize/2;
			} else {
				result = -1000 + focusAreaSize/2;
			}
		} else{
			result = touchCoordinateInCameraReper - focusAreaSize/2;
		}
		return result;
	}

	public float mDist;

    /*
     handle zoom function when user zoom in/out camera preview.
    */
	public void handleZoom(MotionEvent event) {
		if(mCamera == null){
			return;
		}

		Camera.Parameters params = mCamera.getParameters();

		int maxZoom = params.getMaxZoom();
		int zoom = params.getZoom();
		float newDist = getFingerSpacing(event);
		if (newDist > mDist) {
			//zoom in
			if (zoom < maxZoom)
				zoom++;
		} else if (newDist < mDist) {
			//zoom out
			if (zoom > 0)
				zoom--;
		}
		mDist = newDist;
		params.setZoom(zoom);
		mCamera.setParameters(params);
	}

    /*
     get finger distance when user zoom in/out camera preview
    */

	public float getFingerSpacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float)Math.sqrt(x * x + y * y);
	}

    /*
     check zoomable of camera
    */
	public boolean isZoomSupported(){
		if(mCamera != null){
			Camera.Parameters params = mCamera.getParameters();
			return params.isZoomSupported();
		}
		return false;
	}
}
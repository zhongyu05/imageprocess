package com.example.imageprocess;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean isPreviewRunning = false;
    private static final String TAG = "CameraPreview";
    public int needRotate = 0;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            isPreviewRunning = true;
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (isPreviewRunning)
        {
            mCamera.stopPreview();
        }

        Parameters parameters = mCamera.getParameters();
        Display display = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        if(display.getRotation() == Surface.ROTATION_0)
        {
            //parameters.setPreviewSize(height, width);                           
            mCamera.setDisplayOrientation(90);
            mCamera.setParameters(parameters);
            needRotate = 90; //back 90
        }

        if(display.getRotation() == Surface.ROTATION_90)
        {
            //parameters.setPreviewSize(width, height); 
            //mCamera.setDisplayOrientation(90);
            //mCamera.setParameters(parameters);
        	needRotate = 0;
        }

        if(display.getRotation() == Surface.ROTATION_180)
        {
            mCamera.setDisplayOrientation(270);
            mCamera.setParameters(parameters);
            needRotate = 90;
        }

        if(display.getRotation() == Surface.ROTATION_270)
        {
            //parameters.setPreviewSize(width, height);
            mCamera.setDisplayOrientation(180);
            mCamera.setParameters(parameters);
            needRotate = 180;
        }

        
        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            isPreviewRunning = true;

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}

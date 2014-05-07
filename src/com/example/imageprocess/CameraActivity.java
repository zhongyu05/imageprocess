package com.example.imageprocess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.animation.AnimatorSet.Builder;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.imageprocess.CameraPreview;
import com.example.imageprocess.R;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class CameraActivity extends Activity{
	private static final String TAG = "CameraActivity";

	private static int PICK_IMAGE = 1;
	
	private Camera mCamera;
	private CameraPreview mPreview;
	
    private AsyncHttpClient mHttpclient = new AsyncHttpClient();
    private ProgressDialog sendingDialog = null;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cam);
	}

	@Override
	protected void onResume(){
		super.onResume();
        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.removeAllViews();
		if (mCamera!=null){
			preview.addView(mPreview);
		}
	}
	
    protected void onPause() {
        super.onPause();
        releaseCamera();              // release the camera immediately on pause event
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICK_IMAGE && data != null && data.getData() != null) {
        	Uri selectedImage = data.getData();
        	String[] filePathColumn = { MediaStore.Images.Media.DATA };
        	Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
        	cursor.moveToFirst();

        	int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        	final String picturePath = cursor.getString(columnIndex);
            Log.d(TAG, "path:"+picturePath);

        	cursor.close();
        	final Handler handler = new Handler();
        	handler.postDelayed(new Runnable() {
        	  @Override
        	  public void run() {
        	    //Do something after 100ms
        		  startMainActivity(picturePath);
        	  }
        	}, 100);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
	/** Start Main Activity. */
	public void startMainActivity(String imageFilePath){
        Intent audioIntent = new Intent(CameraActivity.this, MainActivity.class);
        audioIntent.setAction("com.example.imageprocess.MAIN");
        audioIntent.putExtra("pic", imageFilePath);
        audioIntent.putExtra("rotate", mPreview.needRotate);
        this.startActivity(audioIntent);
	}

    private void releaseCamera(){
        if (mCamera != null){
        	mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.lock();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.layout.cam_menu, menu);
		return true;
	}
	
	
	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance(){
	    Camera c = null;
	    try {
	        c = Camera.open(); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	        // Camera is not available (in use or does not exist)
	    }
	    return c; // returns null if camera is unavailable
	}
	
	/** Create a File for saving an image or video */
	public static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.
		String storageStatusString = Environment.getExternalStorageState();
        Log.d("ImageProcessStatus", storageStatusString);

        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        path.mkdirs();
	    File mediaStorageDir = new File(path, "imgprocess");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("ImageProcess", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
	
	private void uploadPicture(){
		RequestParams params = new RequestParams();
		String storageStatusString = Environment.getExternalStorageState();
        Log.d("ImageProcessStatus", storageStatusString);

        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
	    File mediaStorageDir = new File(path, "imgprocess");
	    File[] files = mediaStorageDir.listFiles();
	    for (int i = 0; i < files.length; i++) {
	    	File file = files[i];

	    	if (file.isDirectory()) {
				continue;
			}
			try {
				params.put("pictures["+(i+1)+"]", file);
		        Log.d("ImageProcessStatus", "file:"+file.getName());
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		//File imgFile = new File(passedImgPathString);
	    mHttpclient.setTimeout(8000);
		mHttpclient.post(this, "http://cs.rochester.edu/u/zyu/blind/upload.php", params, new AsyncHttpResponseHandler() {
		    @Override
		    public void onSuccess(String response) {
			    sendingDialog.dismiss();
		        Log.d("ImageProcessStatus", "res::"+response);

			    Toast.makeText(getApplicationContext(), "Uploading complete, you can quit the app now.", Toast.LENGTH_SHORT).show();
		    }
		    @Override
		    public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, java.lang.Throwable error){
		    	 Log.v("Main", "fail"+statusCode+responseBody+","+error.toString());
		    	 Toast.makeText(getApplicationContext(), "Sending failed", Toast.LENGTH_SHORT).show();
		    	 sendingDialog.dismiss();
		    }
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    /*case R.id.action_search:
	        newImagePicker();
	        return true;*/
	    case R.id.camera:
	    	mCamera.takePicture(null, null, mPicture);
	    	final MenuItem disableItem = item;
	    	disableItem.setEnabled(false);
			Handler myHandler = new Handler();
			myHandler.postDelayed(new Runnable()
			{
			    @Override
			    public void run()
			    {
			    	disableItem.setEnabled(true);
			    }
			 }, 1000);
	        return true;
	    case R.id.action_upload:
			sendingDialog = ProgressDialog.show(this, "Uploading, please wait",
				    null, true);
			sendingDialog.setOnDismissListener(new OnDismissListener() {
				
				@Override
				public void onDismiss(DialogInterface dialog) {
					// TODO Auto-generated method stub
					mHttpclient.cancelRequests(getApplicationContext(), true);
				}
			});
			
			sendingDialog.show();
			uploadPicture();
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	/** Start an intent to pick an image from the gallery. */
	public void newImagePicker(){
		if (Build.VERSION.SDK_INT < 19) {
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
			startActivityForResult(
					Intent.createChooser(intent, getString(R.string.app_name)),
					PICK_IMAGE);
		}
		else{
			Intent intent = new Intent(
					  Intent.ACTION_PICK,
					  android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
					  );
			intent.setType("image/*");
			startActivityForResult(intent, PICK_IMAGE);
		}
	}
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;

	
	private PictureCallback mPicture = new PictureCallback() {

	    @Override
	    public void onPictureTaken(byte[] data, Camera camera) {

	        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
	        if (pictureFile == null){
	            Log.d(TAG, "Error creating media file, check storage permissions.");
	            return;
	        }

	        try {
	            FileOutputStream fos = new FileOutputStream(pictureFile);
	            fos.write(data);
	            fos.close();
	            startMainActivity(pictureFile.getAbsolutePath());
	            //if (mCamera.getParameters().)
	            
	        } catch (FileNotFoundException e) {
	            Log.d(TAG, "File not found: " + e.getMessage());
	        } catch (IOException e) {
	            Log.d(TAG, "Error accessing file: " + e.getMessage());
	        }
	    }
	};
}

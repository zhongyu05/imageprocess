package com.example.imageprocess;

import java.io.FileNotFoundException;
import java.text.DecimalFormat;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends Activity {
	private static final String TAG = "ImageProcess::Activity";
	
	private ImageView originalImageView = null;
	private ImageView processedImageView = null;
	private Button chooseButton = null;
	private Button cameraButton = null;
	private Bitmap bitmap = null;
	
	private Mat rgbMatOri = null;
	private Mat grayMatOri = null;
	private Mat originalgrayMatOri = null;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
	
    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		originalImageView = (ImageView)findViewById(R.id.original_picture);
		processedImageView = (ImageView)findViewById(R.id.processed_picture);
		
		//setup button listener
		chooseButton = (Button)findViewById(R.id.choose_button);
		chooseButton.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick(View v) {
			        Log.i(TAG,"Choose onTouch event");
					Intent intent = new Intent();
					intent.setType("image/*");
					intent.setAction(Intent.ACTION_GET_CONTENT);
					startActivityForResult(intent, 1);
				}
			}
		);
		
		//setup button listener
		cameraButton = (Button)findViewById(R.id.camera_button);
		cameraButton.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick(View v) {
			        Log.i(TAG,"Camera onTouch event");
					Intent intent = new Intent();
					intent.setType("image/*");
					intent.setAction(Intent.ACTION_GET_CONTENT);
					startActivityForResult(intent, 1);
					// please implement your image processing method here
//					try{
//						
//						Feature feature = Evaluate.doEvaluate(rgbMatOri);
//					}
//					catch(Exception e){
//						Log.e(e.toString(), null);
//			        }
				}
			}
		);
		
		Log.i(TAG, "MainActivity createded successfully");
		
	}
	
	@Override  
    public void onResume(){  
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);  
    }
	
	//read and display the chosen picture
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (RESULT_OK == resultCode) {
	    	Uri uri = data.getData();
	    	ContentResolver cr = this.getContentResolver();
	    	try {
	    		originalImageView.setImageBitmap(null);
	    		originalImageView.destroyDrawingCache();
	    		if (bitmap != null && !bitmap.isRecycled()) {
	    			bitmap.recycle();
	    		}
	    		bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
	    		
	    		// process Image
	    		rgbMatOri = new Mat();
	    		originalgrayMatOri = new Mat();
	    		grayMatOri = new Mat();
	    		Utils.bitmapToMat(bitmap, rgbMatOri);
//Convert the Original Color Image into Gray Image
	    		Imgproc.cvtColor(rgbMatOri, originalgrayMatOri, Imgproc.COLOR_RGB2GRAY);
	    		
	    		Bitmap grayBmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.RGB_565);
//Get Original Size Info
	    		int w = grayBmp.getWidth();
	    		int h = grayBmp.getHeight();

//Resize the Original Input Image if input is too big
	    		if (w*h>512*512)
	    	    {
//Resize according to original aspect ratio
	    		Size dsize = new Size((double)(512*w/h),(double)(512*h/w));
                    try
                    {
                        System.out.println(dsize.height+dsize.width);
                        Imgproc.resize(originalgrayMatOri, grayMatOri, dsize,0,0,Imgproc.INTER_CUBIC);
                    }
                    catch(Exception e)
                    {
                    Log.e(e.toString(), null);
                    }
	    	    }
	    		else
	    		{
	    			Size dsize = new Size(w,h);
	    			try
		            {
	    			Imgproc.resize(originalgrayMatOri, grayMatOri, dsize,0,0,Imgproc.INTER_CUBIC);
		            }
		            catch(Exception e)
		            {
		              Log.e(e.toString(), null);
		            }
		        }
	    		
	    		Utils.matToBitmap(originalgrayMatOri, grayBmp);
	    		Bitmap grayBmpNew = Bitmap.createBitmap(grayMatOri.width(), grayMatOri.height(), Config.RGB_565);
	    		Utils.matToBitmap(grayMatOri, grayBmpNew);  
	            //processedImageView.setImageBitmap(grayBmp);

//Start of the Evaluation Part
	    		try
	            {
	    			TextView tv;
	    			System.out.println("Gray Convert");
//Evaluate Class is the Image process algorithm where the output feature contains all the information needed
	    			Feature feature = Evaluate.doEvaluate(rgbMatOri);
//Display the result of saliency detected object
	    			System.out.println("Saliency Detect Map display");
		            Bitmap result = Bitmap.createBitmap(feature.result.width(), feature.result.height(), Config.RGB_565);    
		    		feature.result.convertTo(feature.result, CvType.CV_8UC1);
		            Utils.matToBitmap(feature.result, result);
		    		//Bitmap result = Bitmap.createBitmap(feature.SaliencyMap.width(), feature.SaliencyMap.height(), Config.RGB_565);    
		    		//feature.SaliencyMap.convertTo(feature.SaliencyMap, CvType.CV_8UC1);
		    		//Utils.matToBitmap(feature.SaliencyMap, result);
		    		
		    		//Feedback for illumination
		    		tv = (TextView) findViewById(R.id.Illumination);
		    		if (feature.illu[0]){
		    		tv.setText("Too Low Illu");
		    		}else if(feature.illu[1]){
		    			tv.setText("Too High Illu");
		    		}else{
		    			tv.setText("Good Illu");
		    		}
		    			
		    		//Feedback for blur
		    		tv = (TextView) findViewById(R.id.FTBlur);
		    		if (feature.blur){
		    		tv.setText("Blurry Image");
		    		}else {
		    			tv.setText("Clear Image");
		    		}
		    		
		    		//Feedback for blurextent
		    		tv = (TextView) findViewById(R.id.Blurdetect);
		    		if (feature.blurextent >=0.85){
		    		tv.setText("Highly Blurry");
		    		} else if (feature.blurextent > 0.5){
		    			tv.setText("Probable Blurry");
		    		}else{
		    			tv.setText("Clearly");
		    		}
		    		//Feedback for close detection
		    		String close = "";
		    		int c = 0;
		    		if (feature.closeflag[0]){
		    			close = close +"Top";
		    			c++;
		    		}
		    		if (feature.closeflag[1]){
		    			close = close +" Bott";
		    			c++;
		    		}
		    		if (feature.closeflag[2]){
		    			close = close +" Left";
		    			c++;
		    		}
		    		if (feature.closeflag[3]){
		    			close = close +" Right";
		    			c++;
		    		}
		    		if (c>=3){
		    			close = "Too Close";
		    		}
		    		
		    		if (c ==0){
		    		 close = "Nice";
		    		}
		    		tv = (TextView) findViewById(R.id.Close);
		    		tv.setText(close);
		    		
		    		//Feedback for multiple object
		    		tv = (TextView) findViewById(R.id.Multiple);
		    		if (feature.multiple){
		    		tv.setText("Multiple Obj");
		    		}else {
		    			tv.setText("Single Obj");
		    		}
		    		
		    		processedImageView.setImageBitmap(result);
		    	}
	            catch(Exception e)
	            {
	              Log.e(e.toString(), null);
	            }
	    		
	    		
	    		originalImageView.setImageBitmap(bitmap);
	    	} catch (FileNotFoundException e) {
	    		e.printStackTrace();
	    	}
	    }
	    super.onActivityResult(requestCode, resultCode, data);
	}
	
	

}

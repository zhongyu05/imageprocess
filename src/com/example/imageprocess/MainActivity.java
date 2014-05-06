package com.example.imageprocess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

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
	private String passedImgPathString = null;
	
	private int needRotate = 0;
	

	
	//private MarvinImage image;
	//private MarvinImagePlugin     imagePlugin;
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    try {
            			processImage();
            		} catch (FileNotFoundException e) {
            			// TODO Auto-generated catch block
            			e.printStackTrace();
            		}
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
		passedImgPathString = getIntent().getStringExtra("pic");
		needRotate = getIntent().getIntExtra("rotate", 0);
		
		/*
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
		);*/
		
		Log.i(TAG, "MainActivity createded successfully");
		
	}
	
	@Override  
    public void onResume(){  
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);  
    }
	
	public static Bitmap RotateBitmap(Bitmap source, float angle)
	{
	      Matrix matrix = new Matrix();
	      matrix.postRotate(angle);
	      return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}
	
	
	private void processImage() throws FileNotFoundException{
    	originalImageView.setImageBitmap(null);
		originalImageView.destroyDrawingCache();
		if (bitmap != null && !bitmap.isRecycled()) {
			bitmap.recycle();
		}
		/*
		image = MarvinImageIO.loadImage(passedImgPathString);
		imagePlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.color.skinColorDetection.jar");
		imagePlugin.process(image, image);
		File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        path.mkdirs();
	    File mediaStorageDir = new File(path, "imgprocess");
	    File tmpSkinFile = new File(mediaStorageDir.getPath() + File.separator + "skintmp.jpg");

		MarvinImageIO.saveImage(image, tmpSkinFile.getAbsolutePath());*/
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		bitmap = BitmapFactory.decodeFile(passedImgPathString, options);
		//if (Camera)
		if (bitmap == null) {
			options.inPreferredConfig = Bitmap.Config.RGB_565;
			bitmap = BitmapFactory.decodeFile(passedImgPathString, options);

		}
		
		bitmap = RotateBitmap(bitmap, needRotate);
		//bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
		
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
			
//		    Bitmap result = Bitmap.createBitmap(feature.superpixel.width(), feature.superpixel.height(), Config.RGB_565);    
//			feature.superpixel.convertTo(feature.superpixel, CvType.CV_8UC3);
//			Utils.matToBitmap(feature.superpixel, result);

			Bitmap result = Bitmap.createBitmap(feature.result.width(), feature.result.height(), Config.RGB_565);    
			feature.result.convertTo(feature.result, CvType.CV_8UC1);
		    Utils.matToBitmap(feature.result, result);
		    
//			Bitmap result = Bitmap.createBitmap(feature.SaliencyMap.width(), feature.SaliencyMap.height(), Config.RGB_565);    
//			feature.SaliencyMap.convertTo(feature.SaliencyMap, CvType.CV_8UC1);
//			Utils.matToBitmap(feature.SaliencyMap, result);
			
			//Feedback for illumination
			tv = (TextView) findViewById(R.id.Feedback);
			String feedback = "";
			if (feature.illu[0]){
				feedback = feedback + "Too Low Illuminated";
			}else if(feature.illu[1]){
				feedback = feedback + "Too High Illuminated";
			}else{
				feedback = feedback + "Good Illu";
			}
				
			//Feedback for blur
			if (feature.blur || feature.blurextent >= 0.85){
				feedback = feedback + " and blurry";
			}
			
			//Feedback for blurextent
			if (feature.blurextent > 0.5 || !feature.blur){
				feedback = feedback + " and probably blurry.\n";
			}
			
			//Feedback for close detection
			int c = 0;
			String closefeedback = "";
			if (feature.closeflag[0]){
				closefeedback = closefeedback + "Objects too close to top; ";
				c++;
			}
			if (feature.closeflag[1]){
				closefeedback = closefeedback + "Objects too close to bottom; ";
				c++;
			}
			if (feature.closeflag[2]){
				closefeedback = closefeedback + "Objects too close to left; ";
				c++;
			}
			if (feature.closeflag[3]){
				closefeedback = closefeedback + "Objects too close to right; ";
				c++;
			}
			if (c>=3){
				closefeedback = "Too Close to Object";
			}
			
			feedback = feedback + closefeedback;
			feedback = feedback + '\n';
			//Feedback for multiple object
			if (feature.multiple){
				feedback = feedback + "Multiple Objects existing, please remove irrelevant objects";
			}
			
			tv.setText(feedback);
			

			
			processedImageView.setImageBitmap(result);
		}
		catch(Exception e)
		{
//		  Log.e(e.toString(), null);
			System.out.println("Exception caught");
		}
		
		
		originalImageView.setImageBitmap(bitmap);
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
					tv = (TextView) findViewById(R.id.Feedback);
					String feedback = "";
					if (feature.illu[0]){
						feedback = feedback + "Too Low Illuminated";
					}else if(feature.illu[1]){
						feedback = feedback + "Too High Illuminated";
					}else{
						feedback = feedback + "Good Illu";
					}
						
					//Feedback for blur
					if (feature.blur || feature.blurextent >= 0.85){
						feedback = feedback + " and blurry";
					}
					
					//Feedback for blurextent
					if (feature.blurextent > 0.5 || !feature.blur){
						feedback = feedback + " and probably blurry.\n";
					}
					
					//Feedback for close detection
					int c = 0;
					String closefeedback = "";
					if (feature.closeflag[0]){
						closefeedback = closefeedback + "Objects too close to top; ";
						c++;
					}
					if (feature.closeflag[1]){
						closefeedback = closefeedback + "Objects too close to bottom; ";
						c++;
					}
					if (feature.closeflag[2]){
						closefeedback = closefeedback + "Objects too close to left; ";
						c++;
					}
					if (feature.closeflag[3]){
						closefeedback = closefeedback + "Objects too close to right; ";
						c++;
					}
					if (c>=3){
						closefeedback = "Too Close to Object";
					}
					
					feedback = feedback + closefeedback;
					feedback = feedback + '\n';
					//Feedback for multiple object
					if (feature.multiple){
						feedback = feedback + "Multiple Objects existing, please remove irrelevant objects";
					}
					
					tv.setText(feedback);
		    		
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

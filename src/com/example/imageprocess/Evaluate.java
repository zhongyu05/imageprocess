package com.example.imageprocess;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.*;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.*;
import org.opencv.utils.Converters;

import android.util.Log;

/*
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import edu.rochester.R;

import java.applet.*; 
import java.awt.Component; 
import java.awt.image.*;  
*/

/*define a class for each .m file*/
public class Evaluate {
	
	/*the following static method is actually doing all the jobs in the evalate.m*/
	public static Feature doEvaluate(Mat imgRGB)
	{
//        Original Size Information
		int oWidth, oHeight;
		Mat I = new Mat();
		Mat copy = new Mat();
		Mat ab = new Mat();
//        Blur Detection Feedback
		boolean blur;
		double blurextent;
//        Illumination
		boolean[] illu = new boolean[2];
//        Close to Boundary CHeck
		boolean[] closeflag = new boolean[4];
//        Multiple Object Detection
		boolean multiple = false;
//        Slaiency Map and three seperate score map
		Mat saliencyMap = new Mat();
		Mat MostSalient = new Mat();
		Mat MediumSalient = new Mat();
		Mat LeastSalient = new Mat();
//        Reliable Saliency Detected Map
		Mat Salient = new Mat();
		Mat I_whole = new Mat();
//        Feedback feature
		Feature feature = new Feature();
		Saliency sal = new Saliency();
		
		System.out.println("Evaluate Start");
		
		try{
		
			imgRGB.copyTo(copy);
			
			System.out.println("Save Copy");
			
			copy.copyTo(feature.originalimage);
			
			System.out.println("Save Original Image");
			
			oWidth = imgRGB.width();
			oHeight= imgRGB.height();
			
			System.out.println("Get Size");
			
			if(oWidth*oHeight > 512*512)
			{
				Imgproc.resize(copy, copy, new Size(Math.round(Math.sqrt(512*512*oWidth/oHeight)), Math.round(Math.sqrt(512*512*oHeight/oWidth))), 0, 0, Imgproc.INTER_CUBIC);		
				//Imgproc.resize(copy,copy,new Size(512,512),0,0,Imgproc.INTER_CUBIC);
				oWidth = copy.width();
				oHeight= copy.height();
			}
			
			System.out.println("Save shrinked image");
			copy.copyTo(feature.shrinked);
			
			Imgproc.cvtColor(copy, I, Imgproc.COLOR_RGB2GRAY);
			

			//Blur Detection Part
			feature.blur = ftblur(I);
			feature.blurextent = blurdetect(I);
            
//            Illumination Check
			feature.illu = illucheck(I);
			System.out.println("Low Illusion Flag is "+illu[0]);
			System.out.println("High Illusion Flag is "+illu[1]);
			
		//Get the Saliency Map
			System.out.println("Start Saliency Map Detection");
			salient(I,saliencyMap,MostSalient,MediumSalient,LeastSalient);

		
		//Get the I_whole Image	
			//Core.add(MostSalient, MediumSalient, Salient);
            System.out.println("Saliency Map Mask Start");

//			Salient = MostSalient.clone();
            MostSalient.copyTo(Salient);
			I.convertTo(I, CvType.CV_32FC1);
			//Fill the Mask to get the I_whole Image Better
            System.out.println("Start filling the Saliency Mask");

			imfill(Salient);
			
		//Find the Convex Hul of the Mask
            System.out.println("Convex Hull Part");
			 Mat boundary = new Mat(I.rows(),I.cols(),CvType.CV_32FC1,Scalar.all(0));
			Convex(Salient,boundary);
//			Salient = boundary.clone();
            boundary.copyTo(Salient);
            System.out.println("Filling Convex Hull");
			imfill(Salient);
			Core.multiply(Salient, I, I_whole);
            System.out.println("Save feature Convex Hull");
			feature.Salient = Salient.clone();
			feature.I_whole = I_whole.clone();
		//Test if the object in the Image is too close to the boundary
            System.out.println("Close to Border Detection");
			feature.closeflag = Close(Salient);
			
		//Check if there are multiple objects in the image
            System.out.println("Multiple Object Detection");
			feature.multiple = Multiple(Salient);
			
			/*example*/
			
				//Imgproc.GaussianBlur(feature.result, feature.result, new Size(5, 5), 0.5);
			
			//feature.result = I.colRange(100, 200);
		//	salient(sal.Saliencymap);
			System.out.println("Saliency Detect over");
			feature.result = I_whole.clone();
			sal.Saliencymap.copyTo(feature.SaliencyMap);
			//feature.SaliencyMap = sal.Saliencymap;
			
			
		}
		 catch(Exception e)
         {
           Log.e(e.toString(), null);
         }
		
		return feature;
	}
	
    
//    This funciton is the original Saliency Map detection for backup
	public static Saliency slainecydetect (Mat I){
		Saliency sal = new Saliency();
		Mat I_shrink = new Mat();
//        Fourier Transform
		Mat F = new Mat();
//        Inverse Fourier Transform
		Mat IF = new Mat();
//        Manitude Map
		Mat magnitude = new Mat();
//        Phase Map
		Mat myAngle = new Mat();
//        Spectral Residual Map and its accompanied map
		Mat mySpectralResidual = new Mat();
		Mat myLogAmplitude = new Mat();
		Mat saliencyMap = new Mat();
		Mat ab = new Mat();
		//System.out.println((double)64.0/I.height());
		try{
//            Resize the input image into a pretty small one
		Imgproc.resize(I, I_shrink, new Size(),(double)64.0/I.cols(),(double)64.0/I.cols(),Imgproc.INTER_LINEAR);
		
		I_shrink.convertTo(I_shrink, CvType.CV_32FC1);
		System.out.println("Starting DFT");
//		Pad the image for fourier transform
		Mat padded = new Mat();
		int m = Core.getOptimalDFTSize(I_shrink.rows());
		int n = Core.getOptimalDFTSize(I_shrink.cols());
		Imgproc.copyMakeBorder(I_shrink, padded, 0, m-I_shrink.rows(), 0, n-I_shrink.cols(), Imgproc.BORDER_CONSTANT,Scalar.all(0));
		padded.convertTo(padded, CvType.CV_32FC1);	

		System.out.println("Start merging"); 
		
//      Fourier Transform Part
		List<Mat> planes = new ArrayList<Mat>();
		planes.add(padded);
		planes.add(Mat.zeros(padded.size(), CvType.CV_32FC1));
		Core.merge(planes, F);
		System.out.println("Merge complete");
		
		
		Core.dft(F, F);
//		Get the Magnitude and Phase map
		Core.split(F, planes);
		
		//Core.magnitude(planes.get(0), planes.get(1), magnitude);
		System.out.println("DFT Finished");
		Core.split(F, planes);
		Core.magnitude(planes.get(0), planes.get(1), magnitude);
		
		//Core.absdiff(F, Scalar.all(0), magnitude);
		//Core.add(magnitude, Scalar.all(1),magnitude);
            
//      Process the Magnitude Map to get the LogAmplitude Map
		Core.log(magnitude, myLogAmplitude);
		System.out.println("Magnitude Get");
            
//      Following comment part is for fftshift realization in case
            
		//Rect r = new Rect(0,0,myLogAmplitude.cols() & -2, myLogAmplitude.rows() & -2);
		//myLogAmplitude = myLogAmplitude.submat(r);
		/*
		int cx = myLogAmplitude.cols()/2;
		int cy = myLogAmplitude.rows()/2;
		
		Mat q0 = myLogAmplitude.submat(new Rect(0,0,cx,cy));
		Mat q1 = myLogAmplitude.submat(new Rect(cx,0,cx,cy));
		Mat q2 = myLogAmplitude.submat(new Rect(0,cy,cx,cy));
		Mat q3 = myLogAmplitude.submat(new Rect(cx,cy,cx,cy));
		
		Mat tmp = new Mat();
		q0.copyTo(tmp);
		q3.copyTo(q0);
		tmp.copyTo(q3);
		
		q1.copyTo(tmp);
		q2.copyTo(q1);
		tmp.copyTo(q2);
		
		Core.normalize(myLogAmplitude, myLogAmplitude, 0, 1, Core.NORM_MINMAX);
		*/
		
//            Get the Phase map
		Core.phase(planes.get(1), planes.get(0), myAngle);
		//Core.phase(F, F, myAngle, false);
		
            
		//Mat average = Mat.ones(3,3,CvType.CV_32FC1);
		//average.mul( Mat.ones(3,3,CvType.CV_32FC1), (double)1/9);	
		//System.out.println("Check for Average Point One "+average.get(0, 0)[0]);
            
//      Blur the LogAmplitude for Residual Map
		Imgproc.blur(myLogAmplitude, mySpectralResidual, new Size(3,3),new Point(-1, -1), Imgproc.BORDER_REPLICATE);
		//Imgproc.filter2D(myLogAmplitude, mySpectralResidual, -1, average);
		//mySpectralResidual = myLogAmplitude + mySpectralResidual;
		
            
            
//		Subtract them for Residual Map
		Core.subtract(myLogAmplitude, mySpectralResidual, mySpectralResidual);
		System.out.println("mySpectralResidual "+mySpectralResidual.get(13, 13)[0]);
		
		Mat tmp = new Mat();
		Mat tmp2 = new Mat();
		mySpectralResidual.copyTo(tmp);
		myAngle.copyTo(tmp2);
		double temp;
		
//      Phase Process
		for (int i =0; i<mySpectralResidual.rows(); i++)
		{
			for (int j = 0; j< mySpectralResidual.cols();j++)
			{
				temp = Math.exp(mySpectralResidual.get(i, j)[0])+Math.cos(myAngle.get(i, j)[0]);
				tmp.put(i, j, temp);
				temp = Math.exp(mySpectralResidual.get(i, j)[0])+Math.sin(myAngle.get(i, j)[0]);
				tmp2.put(i, j, temp);
			}
		}
		
		planes.clear();
		planes.add(tmp);
		planes.add(tmp2);
		
		
//		Inverse Fourier Transform
		Core.merge(planes, IF);
		System.out.println("mySpectralResidual "+mySpectralResidual.get(13, 13)[0]);

		System.out.println("Second Merge complete");
		//Core.exp(IF, IF);
		
		//IF.convertTo(IF, CvType.CV_32FC1);
		System.out.println("Exp complete");
		//Core.idft(IF, saliencyMap);
		Core.dft(IF, saliencyMap,Core.DFT_INVERSE|Core.DFT_COMPLEX_OUTPUT|Core.DFT_SCALE,0);
		//Core.dft(IF, saliencyMap, Core.DFT_INVERSE, 0);
		System.out.println("IDFT complete");
            
		planes.clear();
		Core.split(saliencyMap, planes);
		Core.magnitude(planes.get(0), planes.get(1), saliencyMap);
		Core.pow(saliencyMap, 2, saliencyMap);
		Imgproc.GaussianBlur(saliencyMap, saliencyMap,new Size(9,9), 2.5);
		//Core.normalize(saliencyMap, saliencyMap, 0, 1,Core.NORM_MINMAX);
		
		myLogAmplitude.copyTo(saliencyMap);
            
//      Resize the image back to original input size
		Imgproc.resize(saliencyMap, saliencyMap, new Size(I.width() , I.height()));

		
		saliencyMap.convertTo(saliencyMap, CvType.CV_8UC1);
		} catch (Exception e) {
			 Log.e(e.toString(), null);
		};
		//myLogAmplitude.copyTo(sal.Saliencymap);
		//mySpectralResidual.copyTo(sal.Saliencymap);
	//	mySpectralResidual.copyTo(sal.Saliencymap);
		
		saliencyMap.copyTo(sal.Saliencymap);
		return sal;
		
	}

//    This is the one really used
	public static void salient(Mat I,Mat saliencyMap,Mat MostSalient,Mat MediumSalient,Mat LeastSalient){
		Mat imgInput = new Mat();
//        Fourier Transform
		Mat fft = new Mat();
//        Inverse Fourier Transform
		Mat ifft = new Mat();
//        Magnitude Map
		Mat magnitude = new Mat();
//        Phase Map
		Mat phase = new Mat();
//        Saliency Map related process map
		Mat spectralResidual = new Mat();
		Mat magnitudeLog = new Mat();
		Mat angle = new Mat();

//        Original Input information
		int oWidth = I.width();
		int oHeight = I.height();
//		Resize the Input Image into a small image
		Imgproc.resize(I, imgInput, new Size(),(double)64.0/I.cols(),(double)64.0/I.cols(),Imgproc.INTER_LINEAR);
		
		imgInput.convertTo(imgInput, CvType.CV_32FC1);

//		Pad the image for fourier transform
		Mat padded = new Mat(imgInput.rows(), imgInput.cols(), CvType.CV_32FC1, Scalar.all(0));

		
		// merge for Fourier Transform
		List<Mat> planes = new ArrayList<Mat>();
		planes.add(imgInput);
		planes.add(padded);
		//planes.add(padded);
		//planes.add(Mat.zeros(padded.size(), CvType.CV_32FC1));
		Core.merge(planes, fft);
		System.out.println("Start DFT");
		// dft
		Core.dft(fft, fft);
		System.out.println("DFT end");
		
//        Get the Magnitude Map and Phase Map
		Core.split(fft, planes);
		Core.magnitude(planes.get(0), planes.get(1), magnitude);
		Core.add(magnitude, Scalar.all(1), magnitude);
		Core.log(magnitude, magnitudeLog);		
		
		// get phase
		Core.phase(planes.get(0), planes.get(1), angle);
		
		// filter the Magnitude Map for LogAmplitude
		System.out.println("LogAmplitude Map got");
		Imgproc.blur(magnitudeLog, spectralResidual, new Size(3.0, 3.0), new Point(-1, -1), Imgproc.BORDER_REPLICATE);
//      Get Residual
		Core.subtract(magnitudeLog, spectralResidual, spectralResidual);
				
		// merge for idft
		planes.clear();
		planes.add(spectralResidual);
		planes.add(angle);

		Core.exp(planes.get(0), planes.get(0));
		Mat tmp0 = planes.get(0).clone();
		Mat tmp1 = planes.get(1).clone();
		System.out.println("Phase Map Process");
		cosMat(planes.get(1));
		Core.multiply(planes.get(0), planes.get(1), planes.get(0));
		sinMat(tmp1);
		Core.multiply(tmp0, tmp1, planes.get(1));
		Core.merge(planes, ifft);
//		Inverse DFT
		System.out.println("Inverse DFT Start");
		Core.dft(ifft, ifft, Core.DFT_INVERSE|Core.DFT_COMPLEX_OUTPUT|Core.DFT_SCALE, 0);
		Core.split(ifft, planes);

		Core.magnitude(planes.get(0), planes.get(1), saliencyMap);
		// saliency map
		System.out.println("Saliency Map got");
		Core.pow(saliencyMap, 2.0, saliencyMap);
		Imgproc.GaussianBlur(saliencyMap, saliencyMap,new Size(9,9), 2.5);
				
		MinMaxLocResult minmax = Core.minMaxLoc(saliencyMap);
		double min = minmax.minVal;
		double max = minmax.maxVal;
		Core.multiply(saliencyMap, Scalar.all(255/max), saliencyMap);
		
//		Divide the Saliency Map into three parts based on their score
		System.out.println("KMeans start");
		myKmeans(saliencyMap,MostSalient,MediumSalient,LeastSalient);
		
//		Resize the saliency map back to the original size
		System.out.println("Resize the Saliency Map back into original size");
		Imgproc.resize(saliencyMap, saliencyMap, new Size(oWidth,oHeight));
		Imgproc.resize(MostSalient, MostSalient, new Size(oWidth,oHeight));
		Imgproc.resize(MediumSalient, MediumSalient, new Size(oWidth,oHeight));
		Imgproc.resize(LeastSalient, LeastSalient, new Size(oWidth,oHeight));

	}

	
	public static void myKmeans(Mat saliencyMap,Mat MostSalient,Mat MediumSalient,Mat LeastSalient) {
//        1 Dimentional Map
		Mat ab = new Mat();
//        Original Input Image Size
		int oWidth, oHeight;
		oWidth = saliencyMap.width();
		oHeight= saliencyMap.height();
//        Reshape the Map into 1D
		ab = saliencyMap.reshape (0,oWidth*oHeight);
		ab.convertTo(ab, CvType.CV_32FC1);
        
		// K means starts here divide the map into 3 parts based on the descending order of scores
		
		  Mat labels = new Mat();
		 int clustercount;
		  Mat centers = new Mat();
		  TermCriteria criteria = new TermCriteria(TermCriteria.MAX_ITER|TermCriteria.EPS,15,0.01);
		Core.kmeans(ab, 3, labels, criteria, 5, Core.KMEANS_RANDOM_CENTERS,centers);
		
		ab = labels.reshape(0, oHeight);

		System.out.println("Primitive Kmeans finished");

		ab.convertTo(ab, CvType.CV_32FC1);
		Imgproc.threshold(ab, MostSalient, 1.0, 1.0, Imgproc.THRESH_BINARY);
		Imgproc.threshold(ab, MediumSalient, 0, 1.0, Imgproc.THRESH_BINARY);
		Core.subtract(MediumSalient, MostSalient, MediumSalient);
		Imgproc.threshold(ab, LeastSalient, 0, 1.0, Imgproc.THRESH_BINARY_INV);
	
//		Checking for scores
        System.out.println("Check Average Score of 3 map");
        
		Mat check1 = new Mat();
		Mat check2 = new Mat();
		Mat check3 = new Mat();
		int check1count,check2count,check3count;
		
		Core.multiply(saliencyMap, MostSalient,check1);
		Core.multiply(saliencyMap, MediumSalient,check2);
		Core.multiply(saliencyMap, LeastSalient,check3);


		check1count = Core.countNonZero(MostSalient);
		check2count = Core.countNonZero(MediumSalient);
		check3count = Core.countNonZero(LeastSalient);
		
		double check1value = 0;
		for (int i = 0;i<oHeight;i++)
		{
			for (int j = 0;j<oWidth;j++)
			{
				if (check1.get(i,j)[0] > 0)
						{
						check1value += saliencyMap.get(i,j)[0];
						}
			}
		}
//        Getting the average value of the image
		check1value = check1value/check1count;
		
		double check2value = 0;
		for (int i = 0;i<oHeight;i++)
		{
			for (int j = 0;j<oWidth;j++)
			{
				if (check2.get(i,j)[0] > 0)
						{
						check2value += saliencyMap.get(i,j)[0];
						}
			}
		}
//        Getting the average value of the image
		check2value = check2value/check2count;
		
		double check3value = 0;
		for (int i = 0;i<oHeight;i++)
		{
			for (int j = 0;j<oWidth;j++)
			{
				if (check3.get(i,j)[0] > 0)
						{
						check3value += saliencyMap.get(i,j)[0];
						}
			}
		}
//        Getting the average value of the image
		check3value = check3value/check3count;
//        Rearrange the Map ensure the the MostSalient Map has the highest average score and lowest for LeastSalient
		Mat tmp = new Mat();
		if (Math.max(check1value, Math.max(check2value,check3value)) == check1value)
		{		
		if (check2value < check3value)
		{
			//label 2.0 for most, 1.0 for least, 0 for medium
			Imgproc.threshold(ab, MediumSalient, 0, 1.0, Imgproc.THRESH_BINARY_INV);
			Imgproc.threshold(ab, LeastSalient, 0, 1.0, Imgproc.THRESH_BINARY);
			Core.subtract(LeastSalient, MostSalient, MediumSalient);
			
		}
		}else
		{
			if (check2value > check3value)
			{
				if (check1value > check3value){
					// label 2.0 for medium, 1.0 for most and 0 for least
				Imgproc.threshold(ab, MediumSalient, 1.0, 1.0, Imgproc.THRESH_BINARY_INV);
				Imgproc.threshold(ab, MostSalient, 0, 1.0, Imgproc.THRESH_BINARY);
				Core.subtract(MostSalient, MediumSalient, MediumSalient);
				Imgproc.threshold(ab, LeastSalient, 0, 1.0, Imgproc.THRESH_BINARY_INV);		
				}else{
					//label 2.0 for least, 1.0 for most, 0 for medium
					Imgproc.threshold(ab, LeastSalient, 1.0, 1.0, Imgproc.THRESH_BINARY_INV);
					Imgproc.threshold(ab, MostSalient, 0, 1.0, Imgproc.THRESH_BINARY);
					Core.subtract(MostSalient, LeastSalient, MediumSalient);
					Imgproc.threshold(ab, MediumSalient, 0, 1.0, Imgproc.THRESH_BINARY_INV);		
			}
			}else{
				if (check1value > check3value){
					// label 2.0 for medium, 1.0 for least and 0 for most
				Imgproc.threshold(ab, MediumSalient, 1.0, 1.0, Imgproc.THRESH_BINARY_INV);
				Imgproc.threshold(ab, LeastSalient, 0, 1.0, Imgproc.THRESH_BINARY);
				Core.subtract(LeastSalient, MediumSalient, MediumSalient);
				Imgproc.threshold(ab, MostSalient, 0, 1.0, Imgproc.THRESH_BINARY_INV);		
				}else{
					//label 2.0 for least, 1.0 for medium, 0 for most
					Imgproc.threshold(ab, LeastSalient, 1.0, 1.0, Imgproc.THRESH_BINARY_INV);
					Imgproc.threshold(ab, MediumSalient, 0, 1.0, Imgproc.THRESH_BINARY);
					Core.subtract(MediumSalient, LeastSalient, MediumSalient);
					Imgproc.threshold(ab, MostSalient, 0, 1.0, Imgproc.THRESH_BINARY_INV);		
			}
			}
				
		}
         
        System.out.println("MyKmeans Finished");

	}
	
//    Blur Detection Using Fourier Transform
	public static boolean ftblur(Mat imgInput){
		imgInput.convertTo(imgInput, CvType.CV_32FC1);
//        Pad the Input for Fourier Transform
		Mat padded = new Mat(imgInput.rows(), imgInput.cols(), CvType.CV_32FC1, Scalar.all(0));
		Mat F = new Mat();
		Mat magnitude = new Mat();
		Mat H = new Mat(imgInput.rows(), imgInput.cols(), CvType.CV_32FC1,Scalar.all(1));
		int w = imgInput.width();
		int h = imgInput.height();
		
		boolean blur;
		// merge
		List<Mat> planes = new ArrayList<Mat>();
		planes.add(imgInput);
		planes.add(padded);

		Core.merge(planes, F);
		// dft
		Core.dft(F, F);

		Core.split(F, planes);
		Core.magnitude(planes.get(0), planes.get(1), magnitude);
		
        MinMaxLocResult minmax = Core.minMaxLoc(magnitude);
        double min = minmax.minVal;
        double max = minmax.maxVal;
			
		Core.multiply(magnitude, Scalar.all(255.0/max), magnitude);
		
		double sumvalue = 0;
		for (int i = 0;i<h;i++)
		{
			for (int j = 0;j<w;j++)
			{
				sumvalue += magnitude.get(i,j)[0];
			}
		}
	
		
		Core.divide(magnitude, Scalar.all(sumvalue), magnitude);
//      Create a Mask just segmenting the high frequency part from the FT, where high frequency defined as the 25% border
		for (int i = 0;i<Math.round(0.25*h);i++)
		{
			for (int j = 0;j<w;j++)
			{
				H.put(i, j, 0);
			}
		}
	
		
		for (int i = (int)Math.round(0.75*h);i<h;i++)
		{
			for (int j = 0;j<w;j++)
			{
				H.put(i, j, 0);
			}
		}
		
		for (int j = 0;j<Math.round(0.25*w);j++)
		{
			for (int i = 0;i<h;i++)
			{
				H.put(i, j, 0);
			}
		}
		
		for (int j = (int)Math.round(0.75*w);j<w;j++)
		{
			for (int i = 0;i<h;i++)
			{
				H.put(i, j, 0);
			}
		}
		
		
//		Shift the Magnitude Map ensure the origin is at the cneter
		myfftshift(magnitude);
//      Calculate the high part sum of normalized Magnitude Map
		Core.multiply(magnitude, H, magnitude);
		sumvalue = 0;
		for (int i = 0;i<h;i++)
		{
			for (int j = 0;j<w;j++)
			{
				sumvalue += magnitude.get(i,j)[0];
			}
		}
		if (sumvalue < 0.17)
		{
			blur = true;
		}
			else{
			blur = false;
			}

		return blur;
	}
	
//    Illumination Check Part Using Histogram
	public static boolean[] illucheck(Mat imgInput){
		List<Mat> images = new ArrayList<Mat>();
		images.add(imgInput);
		MatOfInt histSize = new MatOfInt(256);
		final MatOfFloat histRange = new MatOfFloat(0f, 256f);
		boolean accumulate = false;
		Mat hist = new Mat();
		boolean low = false;
		boolean high = false;
		boolean [] illu = new boolean[2];
//      Get the histogram
		Imgproc.calcHist(images, new MatOfInt(0), new Mat(), hist, histSize, histRange, accumulate);
		
//      Checking the 40% of the low illumination part and 40% of the high illumination part sum from the histogram
		Core.divide(hist, Scalar.all(Core.sumElems(hist).val[0]), hist);
		Range colRange = new Range(0,30);
		Mat lowillu = hist.submat(colRange, Range.all());
		Range colRange2 = new Range(226,256);
		Mat highillu = hist.submat(colRange2, Range.all());
		if (Core.sumElems(lowillu).val[0]>=0.4){
			low = true;
			}
		if ( Core.sumElems(highillu).val[0]>=0.4){
			high = true;
		}
		illu[0] = low;
		illu[1] = high;
		return illu;
	}
	
//    Blur Detection Using Haar Wavelet Transform
	public static double blurdetect(Mat imgInput){
//        Row And Column
		int r = Math.round(imgInput.rows()/8)*8;
		int c = Math.round(imgInput.cols()/8)*8;
		double blurextent = 0.0;
		Mat I = new Mat(c,r,CvType.CV_32FC1,Scalar.all(0));
		Imgproc.resize(imgInput, I, new Size(c, r), 0, 0, Imgproc.INTER_CUBIC);
        
//      Convert Mat into Double 2D Array
		double[][] I0 = MatToDouble(I);
//      Wavelet Transform
		double[][] dwt = Haar(I0);
//        Convert the result back into Mat
		DoubleToMat(dwt,I);
		//I  = I.t();
		//dwt = MatToDouble(I);
		r = dwt.length;
		c = dwt[0].length;
			
//      Blur Detection Part
		double[][] HL2 = new double[r/4][c/4];
		double[][] LH2 = new double[r/4][c/4];
		double[][] HH2 = new double[r/4][c/4];
		double[][] EMAP2 = new double[r/4][c/4];
		
		double[][] HL3 = new double[r/2][c/2];
		double[][] LH3 = new double[r/2][c/2];
		double[][] HH3 = new double[r/2][c/2];
		double[][] EMAP3 = new double[r/2][c/2];

		double[][] A = new double[r/8][c/8];
		double[][] HL1 = new double[r/8][c/8];
		double[][] LH1 = new double[r/8][c/8];
		double[][] HH1 = new double[r/8][c/8];
		double[][] EMAP1 = new double[r/8][c/8];
	    for (int i = 0 ; i < r/8; i++) {
	    	for(int j = 0;j<c/8; j++){
	    		A[i][j] = dwt[i][j];
	    		HL1[i][j] = dwt[i][j+c/8];
	    		LH1[i][j] = dwt[i+r/8][j];
	    		HH1[i][j] = dwt[i+r/8][j+c/8];
	    		double temppt = HL1[i][j]*HL1[i][j];
	    		temppt += LH1[i][j]*LH1[i][j];
	    		temppt += HH1[i][j]*HH1[i][j];
	    		EMAP1[i][j] = Math.sqrt(temppt);
	    		
	    	}		     
	    }
	    System.out.println("AA got");
	    Mat AA = new Mat(A.length,A[0].length,CvType.CV_32FC1,Scalar.all(0));
		DoubleToMat(A,AA);

	    for (int i = 0 ; i < r/4; i++) {
	    	for(int j = 0;j < c/4; j++){
	    		
	    		HL2[i][j] = dwt[i][j+c/4];
	    		LH2[i][j] = dwt[i+r/4][j];
	    		HH2[i][j] = dwt[i+r/4][j+c/4];
	    		
	    		double temppt = HL2[i][j]*HL2[i][j];
	    		temppt += LH2[i][j]*LH2[i][j];
	    		temppt += HH2[i][j]*HH2[i][j];
	    		EMAP2[i][j] = Math.sqrt(temppt);
	    	}		     
	    }

	    for (int i = 0 ; i < r/2; i++) {
	    	for(int j = 0;j < c/2; j++){
	    		
	    		HL3[i][j] = dwt[i][j+c/2];
	    		LH3[i][j] = dwt[i+r/2][j];
	    		HH3[i][j] = dwt[i+r/2][j+c/2];
	    		
	    		double temppt = HL3[i][j]*HL3[i][j];
	    		temppt += LH3[i][j]*LH3[i][j];
	    		temppt += HH3[i][j]*HH3[i][j];
	    		EMAP3[i][j] = Math.sqrt(temppt);
	    	}		     
	    }
	    
	    double[][] Emax1 = ordmax(EMAP1,2);
	    double[][] Emax2 = ordmax(EMAP2,4);
	    double[][] Emax3 = ordmax(EMAP3,8);
	        
	    int Nedge = 0;
		int T = 35;
		Mat Edgemap = new Mat(Emax1.length,Emax1[0].length,CvType.CV_32FC1,Scalar.all(1));
		for (int i = 0;i<Emax1.length;i++){
			for (int j = 0;j<Emax1[0].length;j++)
			{
				if ( (Emax1[i][j]>T)||(Emax2[i][j]>T)||(Emax3[i][j]>T)){
					Nedge++;
				}else
				{
					Edgemap.put(i, j, 0);
				}
			}
		}
		
		int Nda = 0;
		int Nrg = 0;
		int Nbrg = 0;
		
		for (int k = 0;k<Emax1.length;k++){
			for (int l = 0;l<Emax1[0].length;l++)
			{
				if ( Edgemap.get(k, l)[0]>0){
					// Rule 2
					if ((Emax1[k][l]<Emax2[k][l])&&(Emax2[k][l]<Emax3[k][l])){
						Nda++;
					}
					//Rule 3
					if ((Emax1[k][l]>Emax2[k][l])&&(Emax2[k][l]>Emax3[k][l]))
			        {
						Nrg++;
						//Rule 5
						if (Emax3[k][l]<T)
						{
							Nbrg++;
						}
			        }

					//Rule 4
					if ((Emax1[k][l]<Emax2[k][l])&&(Emax2[k][l]>Emax3[k][l]))
					{ 
						Nrg=Nrg+1;
					//Rule 5
						if (Emax3[k][l]<T)
						{
							Nbrg++;
						}
			        }
					}
				}
			}
		
		imgInput = imgInput.t();

		Mat Emapcheck = new Mat(EMAP1.length,EMAP1[0].length,CvType.CV_32FC1,Scalar.all(0));
		DoubleToMat(EMAP1,Emapcheck);
		Emapcheck = Emapcheck.t();
		/*
		System.out.println(Nda);
		System.out.println(Nedge);
		System.out.println(Nrg);
		System.out.println(Nbrg);
		*/
		blurextent = (double)Nbrg/Nrg;
		System.out.println("Blur Extent is "+blurextent);
		
		return blurextent;
	}
	
//    SuperPixel Part, which remains to be fulfilled
	public static void superpixel(Mat imgInput,Mat bmap){
		//seg2bmap(imgInput,bmap);
		
		try {
			//SuperPixel s = new SuperPixel();
		//	Seg s = new Seg();
		}catch (Exception e){
			
		}
	}
	
//	Fill the holes inside a binary image
	public static void imfill(Mat I){
//		Mat fill = I.clone();
//		Mat contour = I.clone();
        Mat fill = new Mat();
        Mat contour = new Mat();
        I.copyTo(fill);
        I.copyTo(contour);
        
		Point seed = new Point(1,1);
//		System.out.println(seed.x+ " y axis is "+seed.y);
		contour = padarray(contour,2).clone();
		Imgproc.floodFill(contour, new Mat(), seed, Scalar.all(255));
//		System.out.println(contour.rows()+" contour "+contour.cols());
//		System.out.println(I.rows()+" contour "+I.cols());
		fill = contour.submat(new Rect(2,2,I.cols(),I.rows()));
		Imgproc.threshold(fill, I, 2.0, 1, Imgproc.THRESH_BINARY_INV);
	//	Core.multiply(fill, Scalar.all(255), fill);
		
	}
	
//    Get the convex hull from an image
	public static void Convex(Mat copy,Mat boundary)
	{
//		Mat I = copy.clone();
        Mat I = new Mat();
        copy.copyTo(I);
        
		int w = copy.cols();
		int h = copy.rows();
		I.convertTo(I, CvType.CV_8UC1);
		Mat hierarchy = new Mat(I.rows(),I.cols(),CvType.CV_8UC1,new Scalar(0));
		
	    List<MatOfPoint> contours =new ArrayList<MatOfPoint>();
	    List<MatOfInt> hull = new ArrayList<MatOfInt>(contours.size());

	    Imgproc.findContours(I, contours, hierarchy,Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
	   
	    if (contours.size() > 0){
	    for(int i=0; i<contours.size(); i++){
	    	MatOfInt temp = new MatOfInt();
	        Imgproc.convexHull(contours.get(i), temp, false);
	        hull.add(i,temp);
	        
	        List<Mat> planes = new ArrayList<Mat>();
	        Core.split(contours.get(i), planes);
	        
			MinMaxLocResult minmax = Core.minMaxLoc(planes.get(0));
			double min = minmax.minVal;
			double max = minmax.maxVal;
			double r = max - min;
			 minmax = Core.minMaxLoc(planes.get(1));
			 min = minmax.minVal;
			 max = minmax.maxVal;
			double c = max - min;
			//Eliminate those small areas which should be considered as noise
	        if ((int)(r*c)> (int) (0.01*w*h))
	        {
	        Imgproc.drawContours( boundary, contours, i, new Scalar(1,0,0), 1, 8, hierarchy, 0, new Point() ); 
	        Rect brect = Imgproc.boundingRect(contours.get(i));
	       // Core.rectangle(I, brect, new Scalar(255,0,0));
	        }

	    }
       
	    
	    }

	}
	
//    Close to boundary check
	public static boolean[] Close(Mat mask)
	{
		//The colse flag contains 4 direction check for close:
		//flag[0] for up
		//flag[1] for down
		//flag[2] for left
		//flag[3] for right
		boolean[] closeflag = new boolean[4];
//		Mat I = mask.clone();
        Mat I = new Mat();
        mask.copyTo(I);
        
		int w = mask.cols();
		int h = mask.rows();
		I.convertTo(I, CvType.CV_8UC1);
		Mat hierarchy = new Mat(I.rows(),I.cols(),CvType.CV_8UC1,new Scalar(0));
		
	    List<MatOfPoint> contours =new ArrayList<MatOfPoint>();
	    List<MatOfInt> hull = new ArrayList<MatOfInt>(contours.size());
	//    drawing = Mat.zeros(I.size(), im_gray);

	    Imgproc.findContours(I, contours, hierarchy,Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
	    System.out.println("Now Contours Size is  "+contours.size());
	    if (contours.size() > 0){
	    for(int i=0; i<contours.size(); i++){

	        List<Mat> planes = new ArrayList<Mat>();
	        Core.split(contours.get(i), planes);
	        
			MinMaxLocResult minmax = Core.minMaxLoc(planes.get(0));
			double cmin = minmax.minVal;
			double cmax = minmax.maxVal;
		
			 minmax = Core.minMaxLoc(planes.get(1));
			double rmin = minmax.minVal;
			double rmax = minmax.maxVal;

			if( rmin < h/10)
			{
			    closeflag[0] = true;
			}
			
			if(rmax >h-h/10){
				 closeflag[1] = true;
	    }
			
			if(cmin < w/10){
				 closeflag[2] = true;
			}
			
			if(cmax > w-w/10){
				 closeflag[3] = true;
			}
			
			
			//Eliminate those small areas which should be considered as noise
	      
	     //   Imgproc.threshold(src, dst, thresh, maxval, type)
	     //   System.out.println(title);
	      //  System.out.println(t.channels());

	    	}
	    }
	  //  for (int i = 0 ; i<4;i++)
	//		System.out.println(closeflag[i]);
	  return closeflag;
	}
	
//    Multiple Object Detection
	public static boolean Multiple(Mat mask)
	{
		boolean multiple = false;
//		Mat I = mask.clone();
        Mat I = new Mat();
        mask.copyTo(I);
        
		int w = mask.cols();
		int h = mask.rows();
		I.convertTo(I, CvType.CV_8UC1);
		Mat hierarchy = new Mat(I.rows(),I.cols(),CvType.CV_8UC1,new Scalar(0));
		
	    List<MatOfPoint> contours =new ArrayList<MatOfPoint>();
	    List<MatOfInt> hull = new ArrayList<MatOfInt>(contours.size());

	    Imgproc.findContours(I, contours, hierarchy,Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
	   
	    if (contours.size() > 0){
	   multiple = true;
	        }
	    return multiple;
	}
	
/*
	public static void calculateConvexityDefects()
	{
	    mConvexityDefectsMatOfInt4 = new MatOfInt4();

	    try {
	        Imgproc.convexityDefects(aproximatedContours.get(0), convexHullMatOfInt, mConvexityDefectsMatOfInt4);

	        if(!mConvexityDefectsMatOfInt4.empty())
	        {
	            mConvexityDefectsIntArrayList = new int[mConvexityDefectsMatOfInt4.toArray().length];
	            mConvexityDefectsIntArrayList = mConvexityDefectsMatOfInt4.toArray();
	        }
	    } catch (Exception e) {
	        Log.e("Calculate convex hulls failed.", "Details below");
	        e.printStackTrace();
	    }
	}
	
	*/
	public static Mat padarray(Mat img, int padding){
	
	Mat padded = new Mat(img.rows() + 2*padding, img.cols() + 2*padding, img.type());
	padded.setTo(Scalar.all(0));
	Imgproc.copyMakeBorder(img, padded, padding, padding, padding, padding, Imgproc.BORDER_CONSTANT,Scalar.all(0));

//	img.copyTo(padded.submat(new Rect(padding, padding, img.rows()+padding, img.cols()+padding)));
	return padded;
	}
    
//    fftshift from OpenCV website
	public static void myfftshift(Mat F)
	{

			//F = F.submat( new Rect(0,0,F.cols() & -2, F.rows() & -2));
			
			int cx = F.cols()/2;
			int cy = F.rows()/2;
		
			Mat q0 = F.submat(new Rect(0,0,cx,cy));
			Mat q1 = F.submat(new Rect(cx,0,cx,cy));
			Mat q2 = F.submat(new Rect(0,cy,cx,cy));
			Mat q3 = F.submat(new Rect(cx,cy,cx,cy));
			
			
			Mat tmp = new Mat();
			q0.copyTo(tmp);
			q3.copyTo(q0);
			tmp.copyTo(q3);
			
			q1.copyTo(tmp);
			q2.copyTo(q1);
			tmp.copyTo(q2);
			

			//Core.normalize(myLogAmplitude, myLogAmplitude, 0, 1, Core.NORM_MINMAX);
			
	}
	
//	Calculate the sine value
	public static void sinMat(Mat mat) {
		for (int ii=0; ii<mat.rows(); ii++) {
			for (int jj=0; jj<mat.cols(); jj++) {
				mat.put(ii, jj, Math.sin(mat.get(ii, jj)[0]));
			}
		}
	}
	
//	Calculate the cosine value
	public static void cosMat(Mat mat) {
		for (int ii=0; ii<mat.rows(); ii++) {
			for (int jj=0; jj<mat.cols(); jj++) {
				mat.put(ii, jj, Math.cos(mat.get(ii, jj)[0]));
			}
		}
	}
	
	
//	3 level Haar Wavelet Transform
	 public static double[] L3haar( double[] values )
	  {
	    double[] dwt = new double[values.length];

	    double[] L3 = new double[ values.length/2 ];
	    double[] C3 = new double[ values.length/2 ];
	    
	 
	    double[] L2 = new double[ values.length/4 ];
	    double[] C2 = new double[ values.length/4 ];
	    	    
	    double[] L1 = new double[ values.length/8 ];
	    double[] C1 = new double[ values.length/8 ];

	    for (int i = 0, j = 0; i < values.length; i += 2, j++) {
	    	 L3[j] = (values[i] + values[i+1])*0.7071;
		      C3[j] = (values[i] - values[i+1])*0.7071;
	      //L3[j] = (values[i] + values[i+1])/2;
	      //C3[j] = (values[i] - values[i+1])/2;
	    }
	   
	   
	    for (int i = 0, j = 0; i < L3.length; i += 2, j++) {
	    	 L2[j] = (L3[i] + L3[i+1])*0.7071;
		      C2[j] = (L3[i] - L3[i+1])*0.7071;
		    //  L2[j] = (L3[i] + L3[i+1])/2;
		      //C2[j] = (L3[i] - L3[i+1])/2;
		    }
	    
	   
	    
	    for (int i = 0, j = 0; i < L2.length; i += 2, j++) {
	    	  L1[j] = (L2[i] + L2[i+1])*0.7071;
		      C1[j] = (L2[i] - L2[i+1])*0.7071;
		   //   L1[j] = (L2[i] + L2[i+1])/2;
		     // C1[j] = (L2[i] - L2[i+1])/2;
		    }
	  
	    for (int i = 0 ; i < L1.length; i ++) {
		     
		    	 dwt[i] = L1[i];
		    	 dwt[i+L1.length] = C1[i];
	    }
	    
	    for (int i = 0 ; i < L2.length; i ++) {
		     
	    	 dwt[i+2*L1.length] = C2[i];
	    }
	    
	    for (int i = 0 ; i < L3.length; i ++) {

	    	 dwt[i+2*L1.length+L2.length] = C3[i];
	    }
		

	    return dwt;
	  } // haar_calc

	 
	public static double[][] Haar(double[][] input){
		
		int r = input.length;
		int c = input[0].length;
		    
		double[ ][ ] dwt = new double[ r ][ c ];
		    
		    for( int i = 0; i <r; i++ ) {
		      
		      double[ ] arrTime = new double[ c ];
		      
		      for( int j = 0; j < c; j++ )
		        arrTime[ j ] = input[ i ][ j ];
		      
		      double[ ] arrHilb = L3haar( arrTime );
		      
		      for( int j = 0; j < c; j++ )
		        dwt[ i ][ j ] = arrHilb[ j ];
		      
		    } // rows
		    
		    for( int j = 0; j < c; j++ ) {
		      
		      double[ ] arrTime = new double[ r ];
		      
		      for( int i = 0; i < r; i++ )
		        arrTime[ i ] = dwt[ i ][ j ];
		      
		      double[ ] arrHilb = L3haar( arrTime );
		      
		      for( int i = 0; i < r; i++ )
		        dwt[ i ][ j ] = arrHilb[ i ];
		      
		    } // cols
		    
	return dwt;	
	}
	
//    Find the maximum value in a certain mask, non-linear filter for Image Processing
	public static double[][] ordmax( double[][] vals,int ordsize )
	  {
		int r = vals.length;
		int c = vals[0].length;
	    double[][] pad = new double[r+ordsize-1][c+ordsize-1];
	    double[][] temp = new double[r+ordsize-1][c+ordsize-1];
	    double[][] max = new double[r][c];
		double[][] maxroi = new double[Math.round((r+ordsize-1)/ordsize)][Math.round((c+ordsize-1)/ordsize)];
	    
	    for (int i = 0;i<r;i++){
	    	for (int j = 0;j<c;j++){
	    		pad[i][j] = vals[i][j];
	    		temp[i][j] = vals[i][j];
	    	}
	    }
	    //System.out.println(r);
	
	    for (int i = 0;i<r;i++){
	    	for (int j = 0;j<ordsize-1;j++){
	    		pad[i][j+c] = vals[i][c-j-1];
	    		temp[i][j+c] = vals[i][c-j-1];
	    	}
	    }
	    
	    for (int i = 0;i<ordsize-1;i++){
	    	for (int j = 0;j<c+ordsize-1;j++){
	    		pad[i+r][j] = pad[r-i-1][j];
	    		temp[i+r][j] = temp[r-i-1][j];
	    	}
	    }
	    
	    for (int i = 0;i<r;i++){
	    	for (int j = 0;j<c;j++){
	    		double tempmax = pad[i][j];
	    		for (int k = 0;k <ordsize;k++)
	    		{
	    			for (int l = 0;l<ordsize;l++)
	    			{
	    				tempmax = Math.max(tempmax, pad[i+k][j+l]);
	    			}
	    		}
	    		max[i][j] = tempmax;
	    	}
	    }
	   
	    for (int i = 0;i<maxroi.length;i++){
	    	for (int j = 0;j<maxroi[0].length;j++){
	    		maxroi[i][j] = max[ordsize*i][ordsize*j];
	    	}
	    }
	/*    
	    for (int i = 0;i<maxroi.length;i++){
	    	for (int j = 0;j<maxroi[0].length;j++){
	    		System.out.println(maxroi[i][j]);
	    	}
	    }
	 */   
		return maxroi;
	  } // wavelet_test
	
//    Convert the Mat into a double Array
	public static double[][] MatToDouble(Mat I){
		int r = I.rows();
		int c = I.cols();
		double[][] d = new double[r][c];
		
		for (int i = 0;i<r; i++)
		{
			for (int j = 0;j<c; j++)
			{
				d[i][j] = I.get(i, j)[0];
			}
		}
		
		return d;
		
	}
	
//    Convert a 2D double Array back into Mat
	public static void DoubleToMat(double[][] d, Mat I){
		int r = d.length;
		int c = d[0].length;
		//Mat I = new Mat();
		
		for (int i = 0;i<r; i++)
		{
			for (int j = 0;j<c; j++)
			{
				I.put(i, j, d[i][j]);
			}
		}
		
	//	return I;
		
	}
//    Print mat for Debugging purpose
	public static void printMat(Mat mat) {
		for (int ii=0; ii<mat.rows(); ii++) {
			for (int jj=0; jj<mat.cols(); jj++) {
				System.out.print(mat.get(ii, jj)[0] + "	");
			}
			System.out.print("\n");
		}
	}
	
	

}
class Saliency {
	static Mat Saliencymap = new Mat();
}

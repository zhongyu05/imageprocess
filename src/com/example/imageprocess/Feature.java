package com.example.imageprocess;

import org.opencv.core.Mat;


public class Feature{
	
		Mat originalimage;
		Mat SaliencyMap;
		Mat shrinked;
		Mat result;
		boolean blur;
		double blurextent;
		boolean[] illu ;
		boolean[] closeflag ;
		boolean multiple;

		Mat MostSalient ;
		Mat MediumSalient ;
		Mat LeastSalient ;
		Mat Salient ;
		Mat I_whole ;

		/* whatever features you want to add*/
		
		public Feature()
		{
			originalimage = new Mat();
			shrinked = new Mat();
			result = new Mat();
			SaliencyMap = new Mat();
			Mat MostSalient = new Mat();
			Mat MediumSalient = new Mat();
			Mat LeastSalient = new Mat();
			Mat Salient = new Mat();
			Mat I_whole = new Mat();
			
			boolean[] illu = new boolean[2];
			boolean[] closeflag = new boolean[4];
			/* here initialize all your features declared above*/
		}
}
package com.example.imageprocess;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opencv.core.*;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.*;
import org.opencv.utils.Converters;

public class SLICSuperpixel {
//	protected ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();
//	protected ArrayList<ArrayList<Float>> distances = new ArrayList<ArrayList<Float>>();
	protected int[][] clusters;
	protected float[][] distances;
	protected ArrayList<ColorRep> centers = new ArrayList<SLICSuperpixel.ColorRep>();
	protected ArrayList<Integer> centerCounts = new ArrayList<Integer>();
    
	protected Mat image = new Mat();
	protected int K;
	protected int S;
	protected int m;
	protected int maxIterations;
    
	protected boolean withinRange( int x, int y ){
		    return x >= 0 && y >= 0 && x < image.cols() && y < image.rows();
	}

	public void clear() {
//	    clusters.clear();
//	    distances.clear();
	    centers.clear();
	    centerCounts.clear();
	    
	    if( image != null && !image.empty() ){
	        image.release();
	        image = new Mat();
	    }
	}
	
	public void generateSuperPixels(){
		
//    	ArrayList<float> Infiniti = new ArrayList<float>();
//    	for (int i = 0;i < distances.get(0).size();i++) Infiniti.add(float.MAX_VALUE);


		/* Repeat until we hit max iterations (or certain threshold in literature) */
	    for( int iter = 0; iter < this.maxIterations; iter++ ) {
	        
	        /* Reset distances */
//	        for( ArrayList<Float> dist: distances ){
//	        	int dist_size = dist.size();
//	        	dist.clear();
//	        	for (int i = 0;i < dist_size;i++) dist.add(Float.MAX_VALUE);
////	            dist.assign( dist.size(), float.MAX_VALUE );
//	        }
//	        for( int x = 0; x < image.rows();x++){
//	        	for (int y = 0; y < image.cols();y++)
	    	for( int y = 0; y < image.rows();y++){
	    	     for (int x = 0; x < image.cols();x++)
	        		distances[x][y] = Float.MAX_VALUE;
//	        		distances[x][y] = Float.MAX_VALUE;

	        }
	        
	        /* For each cluster centers Ck */
	        for( int k = 0; k < centers.size(); k++ ){
	            ColorRep center = centers.get(k);
	            /* For each 2 x Steps region around Ck */
	            for (int y = (int)center.y - S; y <= (int)center.y + S;y++){
	            	 for( int x = (int)center.x - S; x < (int)center.x + S; x++ ){
		                    if( withinRange(x, y) ){
		                    	float[] color = new float[3];
		                    	image.get(x, y, color);
//		                        Vec3b color = ptr[x];
		                        
		                        /* Compute and retain the smaller distance */
		                        float distance = calcDistance( center, color, x, y );
//		                        float distance = calcDistance( center, color, y, x );

		                        if( distance < distances[x][y] ) {
//		                        if (distance < distances.get(x).get(y) ) {
		                    
//		                        	ArrayList<Float> temp_dist = distances.get(x);
//		                        	temp_dist.set(y, distance);
		                            distances[x][y] = distance;
//		                        	ArrayList<Integer> temp_cluster = clusters.get(x);
//		                        	temp_cluster.set(y, k);
		                            clusters[x][y]  = k;
		                        }
		                    }
		                }
	            }
	            /*
	            tbb::parallel_for( (int)center.y - S, (int)center.y + S, [&](int y) {
	            	float[] ptrdata = new float[];
	            	
//	                Vec3b * ptr = image.ptr<Vec3b>(y);
	                
	                for( int x = center.x - S; x < center.x + S; x++ ){
	                    if( withinRange(x, y) ){
	                        Vec3b color = ptr[x];
	                        
	                         Compute and retain the smaller distance 
	                        float distance = calcDistance( center, color, x, y );
	                        if( distance < distances[y][x] ) {
	                            distances[y][x] = distance;
	                            clusters[y][x]  = k;
	                        }
	                    }
	                }
	            });
	            */
	        }
	        
	        
//	        centers.assign( centers.size(), ColorRep() );
//	        centerCounts.assign( centerCounts.size(), 0 );
	        int centers_size = centers.size();
	        centers.clear();
	        for (int i = 0;i < centers_size;i++) centers.add(new ColorRep());
	        
	        int centerCounts_size = centers.size();
	        centerCounts.clear();
	        for (int i = 0;i < centerCounts_size;i++) centerCounts.add(0);
	        
	        /* Update new cluster centers ... */
//	        for( int y = 0; y < image.cols(); y++ ) {
//	            for( int x = 0; x < image.rows(); x++ ) {
	    	for( int x = 0; x < image.cols(); x++ ) {
	    	    for( int y = 0; y < image.rows(); y++ ) {
	                int cluster_id = clusters[x][y];
//	            	int cluster_id = clusters.get(x).get(y);
	                if( cluster_id > -1 ) {
	                	float[] color = new float[3];
	                	image.get(x, y, color);
//	                    Vec3b color = image.at<Vec3b>(y, x);
	                	ColorRep color_temp = centers.get(cluster_id);
	                	color_temp.add(color, x, y);
//	                	color_temp.add(color, y, x);

//	                    centers[cluster_id].add( color, x, y );
	                	centerCounts.set(cluster_id, centerCounts.get(cluster_id)+1);
//	                    centerCounts[cluster_id]++;
	                }
	            }
	        }
	        
	        /* ... average them */
	        for( int i = 0; i < centers.size(); i++ )
	        {
	        	ColorRep color_temp = centers.get(i);
            	color_temp.div(centerCounts.get(i));
//	            centers[i].div( centerCounts[i] );

	        }
	    }

	}
	
	public Mat getImage() {
	    return image.clone();
	}
	
	public Mat recolor() {
	    Mat temp = image.clone();
//	    ArrayList<Float> color1 = new ArrayList<Float>(centers.size());
//	    ArrayList<Float> color2 = new ArrayList<Float>(centers.size());
//	    ArrayList<Float> color3 = new ArrayList<Float>(centers.size());
	    ArrayList<Float> color1 = new ArrayList<Float>();
	    ArrayList<Float> color2 = new ArrayList<Float>();
	    ArrayList<Float> color3 = new ArrayList<Float>();
	    System.out.println("For recolor part centers has size " + centers.size());
	    for (int i = 0;i < centers.size();i++){
	    	color1.add((float) 0.0);
	    	color2.add((float) 0.0);
	    	color3.add((float) 0.0);

	    }
//	    vector<Vec3f> colors( centers.size() );
	    System.out.println("Super Pixel Accumulate the colors for each cluster");
	    /* Accumulate the colors for each cluster */
//	    for( int y = 0; y < temp.cols(); y++ ) {
	    for( int x = 0; x < temp.cols(); x++ ) {
//	        Vec3b * ptr = temp.ptr<Vec3b>(y);
//	        for( int x = 0; x < temp.rows(); x++ ){
		    for( int y = 0; y < temp.rows(); y++ ){
	        	float[] colors = new float[3];
	        	temp.get(x, y, colors);
	        	int index = clusters[x][y];
//	        	int index = clusters.get(x).get(y);
//	        	int index = clusters.get(x).get(y);
	        	if (index < 0 | index > centers.size()) continue;
	        	color1.set(index, color1.get(index)+colors[0]);
	        	color2.set(index, color2.get(index)+colors[1]);
	        	color3.set(index, color3.get(index)+colors[2]);

	        }
//	            colors[clusters[y][x]] += ptr[x];
	    }
	    System.out.println("Super Pixel Get the average of the colors");
	    /* Get the average of the colors */
	    for( int i = 0; i < color1.size(); i++ ){
	    	color1.set(i, color1.get(i)/centerCounts.get(i));
        	color2.set(i, color2.get(i)/centerCounts.get(i));
        	color3.set(i, color3.get(i)/centerCounts.get(i));
//	        colors[i] /= centerCounts[i];
	    }
	    System.out.println("Super Pixel Recolor the original CIELab image");
	    /* Recolor the original CIELab image with the average color for each clusters */
//	    for( int y = 0; y < temp.cols(); y++ ) {
		for( int x = 0; x < temp.cols(); x++ ) {

//	        Vec3b * ptr = temp.ptr<Vec3b>(y);
//	        for( int x = 0; x < temp.rows(); x++ ) {
		    for( int y = 0; y < temp.rows(); y++ ) {

//	        	int cluster_index = clusters.get(x).get(y);
	        	int cluster_index = clusters[x][y];
	        	if (cluster_index < 0 | cluster_index > centers.size()) continue;

	        	float[] color = new float[3];
	        	color[0] = color1.get(cluster_index);
	        	color[1] = color2.get(cluster_index);
	        	color[2] = color3.get(cluster_index);
	        	temp.put(x, y, color);
	        	
//	            int cluster_index = clusters[y][x];
//	            Vec3b color = colors[cluster_index];
//	            ptr[x] = Vec3b( color[0], color[1], color[2] );
	        }
	    }
	    System.out.println("Super Pixel Recolor finish");

	    return temp.clone();
	}
	
	public Mat drawContours(Mat input){
		
		Mat temp = input.clone();
		
		final int dx[] = { -1, -1, 0, 1, 1, 1, 0, -1 };
	    final int dy[] = { 0, -1, -1, -1, 0, 1, 1, 1 };
	    float[] color = new float[4];
	    color[0] = (float) 255.0;
	    color[1] = (float) 0.0;
	    color[2] = (float) 255.0;
	    
	    boolean[][] taken = new boolean[image.cols()][image.rows()];


//	    for (int x = 0; x < image.rows(); x++){
//	    	for( int y = 0; y < image.cols(); y++ ){
	    for (int y = 0; y < image.rows(); y++){
	    	for( int x = 0; x < image.cols(); x++ ){
//	    		taken.get(x).add ( false );
	    		taken[x][y] = false;
	        }
	    }
	    
	    
//	    for( int y = 0; y < image.cols(); y++ ){
//	        for( int x = 0; x < image.rows(); x++ ) {
	    for( int x = 0; x < image.cols(); x++ ){
	    	for( int y = 0; y < image.rows(); y++ ) {
	            int nr_p = 0;
	            
	            for(int k = 0; k < 8; k++ ) {
	                int nx = x + dx[k];
	                int ny = y + dy[k];
	                
	                if( withinRange( nx, ny ) ){
	                    if( !taken[nx][ny] && clusters[x][y] != clusters[nx][ny] && clusters[x][y] >= 0 && clusters[nx][ny] >= 0 ) {
//		                if( !taken.get(ny).get(nx) && clusters.get(y).get(x) != clusters.get(ny).get(nx) ) {
//			            if( !taken.get(nx).get(ny) && clusters.get(x).get(y) != clusters.get(nx).get(ny) ) {

	                    	nr_p++;
	                        
	                        if( nr_p > 1 )
	                            break;
	                    }
	                }
	            }
	            
	            if( nr_p > 1 ) {
	                
		        	temp.put(y, x, color);
	                taken[x][y] = true;
	            }
	        }
	    }
		
		
		return temp.clone();
	}
	
	public ArrayList<Point> getContours() {
	    final int dx[] = { -1, -1, 0, 1, 1, 1, 0, -1 };
	    final int dy[] = { 0, -1, -1, -1, 0, 1, 1, 1 };
	    
//	    ArrayList<ArrayList<Boolean>> taken = new ArrayList<ArrayList<Boolean>>();
//	    boolean[][] taken = new boolean[image.rows()][image.cols()];
	    boolean[][] taken = new boolean[image.cols()][image.rows()];

//	    ArrayList<Boolean> all_false = new ArrayList<Boolean>();
//	    for (int y = 0;y < image.rows();y++) all_false.add(false);
	    
//	    for (int x = 0; x < image.rows(); x++){
//	    	
//	    		taken.add ( new ArrayList<Boolean>() );
//	        
//	    }
	    
//	    for (int x = 0; x < image.rows(); x++){
//	    	for( int y = 0; y < image.cols(); y++ ){
	    for (int y = 0; y < image.rows(); y++){
	    	for( int x = 0; x < image.cols(); x++ ){
//	    		taken.get(x).add ( false );
	    		taken[x][y] = false;
	        }
	    }
	    
	    ArrayList<Point> contours = new ArrayList<Point>();
	    
//	    for( int y = 0; y < image.cols(); y++ ){
//	        for( int x = 0; x < image.rows(); x++ ) {
	    for( int x = 0; x < image.cols(); x++ ){
	    	for( int y = 0; y < image.rows(); y++ ) {
	            int nr_p = 0;
	            
	            for(int k = 0; k < 8; k++ ) {
	                int nx = x + dx[k];
	                int ny = y + dy[k];
	                
	                if( withinRange( nx, ny ) ){
	                    if( !taken[nx][ny] && clusters[x][y] != clusters[nx][ny] && clusters[x][y] >= 0 && clusters[nx][ny] >= 0 ) {
//		                if( !taken.get(ny).get(nx) && clusters.get(y).get(x) != clusters.get(ny).get(nx) ) {
//			            if( !taken.get(nx).get(ny) && clusters.get(x).get(y) != clusters.get(nx).get(ny) ) {

	                    	nr_p++;
	                        
	                        if( nr_p > 1 )
	                            break;
	                    }
	                }
	            }
	            
	            if( nr_p > 1 ) {
	                contours.add( new Point(x, y) );
//	                ArrayList<Boolean> temp_taken = taken.get(x);
//	                temp_taken.set(y, true);
	                taken[x][y] = true;
	            }
	        }
	    }
	    
	    return contours;
	}

	public ArrayList<Point> getClusterCenters() {
	    ArrayList<Point> result = new ArrayList<Point>(centers.size() );
	    
	    for( int i = 0; i < centers.size(); i++ ) {
	    	Point temp = result.get(i);
	    	double[] point_value = new double[2];
	    	point_value[0] = centers.get(i).x;
	    	point_value[1] = centers.get(i).y;
	    	temp.set(point_value);
	    }
	    
	    return result;
	}

	
	public float calcDistance( ColorRep c, float[] p, int x, int y ) {
	    float d_lab = ( (c.l - p[0]) * (c.l - p[0]) + (c.a - p[1]) * (c.a - p[1]) + (c.b - p[2]) * (c.b - p[2]) );
	    float d_xy  = ( (c.x - x) * (c.x - x) + (c.y - y) * (c.y - y)  );
	    return (float) Math.sqrt( d_lab + d_xy / (S * S) * (m * m) );
	}

	
	public Point findLocalMinimum( Mat image, Point center ){
		
		Point minimum = new Point( center.x, center.y );
	    float min_gradient = Float.MAX_VALUE;
	    for( int y = (int)(center.y - 1); y < (int)center.y + 2; y++ ) {
	        for( int x = (int)(center.x - 1); x < (int)center.x + 2; x++ ) {
	        	float[] lab = new float[3];
//	        	image.get(y, x, lab);
	        	image.get(x, y, lab);

	        	float[] lab_dy = new float[3];
	        	float[] lab_dx = new float[3];

//	        	image.get(y+1, x, lab_dy);
	        	image.get(x+1, y, lab_dx);

//	        	image.get(y, x+1, lab_dx);
	        	image.get(x, y+1, lab_dy);

//	            Vec3b lab    = image.at<Vec3b>( y  , x   );
//	            Vec3b lab_dy = image.at<Vec3b>( y+1, x   );
//	            Vec3b lab_dx = image.at<Vec3b>( y  , x+1 );
	            
	            float diff = Math.abs( lab_dy[0] - lab[0] ) + Math.abs( lab_dx[0] - lab[0] );
	            if( diff < min_gradient ) {
	                min_gradient = diff;
	                minimum.x = x;
	                minimum.y = y;
	            }
	        }
	    }
	    
	    return minimum;
	}
	
	public void init(Mat src, int no_of_superpixels, int m , int max_iterations){
		this.clear();
	    
	    /* Grid interval (S) = sqrt( N / k ) */
	    this.S = (int) (Math.sqrt( (1.0 * src.rows() * src.cols()) / no_of_superpixels ));
	    this.K = no_of_superpixels;
	    this.m = m;
	    this.maxIterations = max_iterations;
	    
	    this.image = src.clone();
//	    toFile(image,"Superpixel_Input");
	    
//		Imgproc.cvtColor(src, image, Imgproc.COLOR_RGB2Lab);
	    image = rgb2lab(src);
//	    toFile(image,"Superpixel_Input_LAB");

//		Imgproc.cvtColor(image, image, Imgproc.COLOR_Lab2LBGR);
//	    toFile(image,"Superpixel_Input_LAB2BGR");

//		Imgproc.cvtColor(image, image, Imgproc.COLOR_Lab2RGB);

//	    Highgui.imwrite("/Users/jingweiguo/Documents/OpenCV/results/SuperPixel_Input.jpg", image);
	    
//	    Imgproc.cvtColor( src, image, Imgproc.CV_BGR2Lab );
		 System.out.println("Superpixel Segmentation Image Input");

	    /* Initialize cluster centers Ck and move them to the lowest gradient position in 3x3 neighborhood */
	    for( int y = S; y < image.rows() - S / 2; y += S ) {
	        for( int x = S; x < image.cols() - S / 2; x += S ) {
//	    for( int x = S; x < image.rows() - S / 2; x += S ) {
//	    	for( int y = S; y < image.cols() - S / 2; y += S ) {
	            Point minimum = findLocalMinimum( image,new Point(x, y));
	            float[] color = new float[3];
	            image.get((int)minimum.x, (int)minimum.y, color);
//	            Vec3b color = image.at<Vec3b>( minimum.y, minimum.x );
	            centers.add(new ColorRep( color, minimum ));
//	            centers.push_back( ColorRep( color, minimum ) );
	        }
	    }

	    /* Set labels to -1 and distances to infinity */
//	    ArrayList<Integer> minus_one = new ArrayList<Integer>();
//	    ArrayList<Float> Infiniti = new ArrayList<Float>();
//	    for (int y = 0; y < image.rows();y++) Infiniti.add(Float.MAX_VALUE);
//	    for (int y = 0; y < image.rows();y++) minus_one.add(-1);
	    
//	    for (int x = 0; x < image.rows();x++){
//	    	clusters.add(new ArrayList<Integer>(image.cols()));
//	    	distances.add(new ArrayList<Float>(image.cols()));
//	    }
//	    distances = new float[image.rows()][image.cols()];
//	    clusters = new int[image.rows()][image.cols()];
	    distances = new float[image.cols()][image.rows()];
	    clusters = new int[image.cols()][image.rows()];
	    
//	    for (int x = 0; x < image.rows();x++){
//	    	for( int y = 0; y < image.cols(); y++ ) {
	    for (int y = 0; y < image.rows(); y++){
	    	for( int x = 0; x < image.cols(); x++ ) {
//	    		clusters.get(x).set(y,-1);
//	    		distances.get(x).set(y,Float.MAX_VALUE);
	    		distances[x][y] = Float.MAX_VALUE;
	    		clusters[x][y] = -1;
//	        clusters.push_back ( ArrayList<Integer>( image.cols(), -1 ) );
//	        distances.push_back( ArrayList<float>( image.cols(), float.MAX_VALUE ) );
	    	}
	    }
	    
	    
	    centerCounts = new ArrayList<Integer>();
	    for (int y = 0; y < centers.size();y++) centerCounts.add(0);

//	    		( centers.size(), 0 );
		 System.out.println("Superpixel Segmentation Initialization finish");

	}
	
	public SLICSuperpixel( Mat src, int no_of_superpixels){
	 int m = 10, max_iterations = 10;
	 System.out.println("Start Superpixel Segmentation");
	 init( src, no_of_superpixels, m, max_iterations );
	}
	
	public SLICSuperpixel( Mat src, int no_of_superpixels, int m, int max_iterations ){
		 init( src, no_of_superpixels, m, max_iterations );
	}

	public Mat rgb2lab(Mat input){
		Mat temp = new Mat();
		temp = input.clone();
		
		List<Mat> planes = new ArrayList<Mat>();
//		Mat channel[] = new Mat[3];
		

		Core.split(temp, planes);
		Mat r = planes.get(0).clone();
		Mat g = planes.get(1).clone();
		Mat b = planes.get(2).clone();
		planes.clear();
	    toFile(r,"Superpixel_Input_r");
	    toFile(g,"Superpixel_Input_g");
	    toFile(b,"Superpixel_Input_b");

//		MinMaxLocResult minmax = Core.minMaxLoc(b);
//		double b_max = minmax.maxVal;
//		
//		minmax = Core.minMaxLoc(g);
//		double g_max = minmax.maxVal;
//
//		minmax = Core.minMaxLoc(r);
//		double r_max = minmax.maxVal;
		
//		if (b_max >255 || g_max > 255 || r_max > 255){
//			Core.multiply(b, Scalar.all(1/255.0), b);
//			Core.multiply(g, Scalar.all(1/255.0), g);
//			Core.multiply(r, Scalar.all(1/255.0), r);
//		}
		
//		Mat lab = new Mat().zeros(temp.rows(), temp.cols(), CvType.CV_8UC1).clone();
		Mat lab_l = new Mat().zeros(temp.rows(), temp.cols(), CvType.CV_8UC1).clone();
		Mat lab_a = new Mat().zeros(temp.rows(), temp.cols(), CvType.CV_8UC1).clone();
		Mat lab_b = new Mat().zeros(temp.rows(), temp.cols(), CvType.CV_8UC1).clone();
		Mat temp1 = new Mat();
		Mat temp2 = new Mat();
		Mat temp3 = new Mat();
		
//		l = R*0.3811 + G*0.5783 + B*0.0402;
		Core.multiply(r, Scalar.all(0.3811), temp1);
		Core.multiply(g, Scalar.all(0.5783), temp2);
		Core.multiply(b, Scalar.all(0.0402), temp3);
		Core.add(temp1, temp2, lab_l);
		Core.add(lab_l, temp3, lab_l);

//		m = R*0.1967 + G*0.7244 + B*0.0782;
		Core.multiply(r, Scalar.all(0.1967), temp1);
		Core.multiply(g, Scalar.all(0.7244), temp2);
		Core.multiply(b, Scalar.all(0.0782), temp3);
		Core.add(temp1, temp2, lab_a);
		Core.add(lab_a, temp3, lab_a);
		
//		s = R*0.0241 + G*0.1288 + B*0.8444;
		Core.multiply(r, Scalar.all(0.0241), temp1);
		Core.multiply(g, Scalar.all(0.1288), temp2);
		Core.multiply(b, Scalar.all(0.8444), temp3);
		Core.add(temp1, temp2, lab_b);
		Core.add(lab_b, temp3, lab_b);
		
//		logl = log10(l);
		Core.log(lab_l,lab_l);
		Core.multiply(lab_l, Scalar.all(1/Math.log(10)), lab_l);
		
//		logm2 = log10(m);
		Core.log(lab_a,lab_a);
		Core.multiply(lab_a, Scalar.all(1/Math.log(10)), lab_a);
		
//		logs = log10(s);
		Core.log(lab_b,lab_b);
		Core.multiply(lab_b, Scalar.all(1/Math.log(10)), lab_b);
		
//		l = logl*0.5774 + logm2*0.5774 + logs*0.5774;
		Core.multiply(lab_l, Scalar.all(0.5774), temp1);
		Core.multiply(lab_a, Scalar.all(0.5774), temp2);
		Core.multiply(lab_b, Scalar.all(0.5774), temp3);
		Core.add(temp1, temp2, lab_l);
		Core.add(lab_l, temp3, lab_l);
		
//		a = logl*0.4082 + logm2*0.4082 - logs*0.8165;
		Core.multiply(lab_l, Scalar.all(0.4082), temp1);
		Core.multiply(lab_a, Scalar.all(0.4082), temp2);
		Core.multiply(lab_b, Scalar.all(-0.8165), temp3);
		Core.add(temp1, temp2, lab_a);
		Core.add(lab_a, temp3, lab_a);
		
//		b = logl*0.7071 - logm2*0.7071;
		Core.multiply(lab_l, Scalar.all(0.7071), temp1);
		Core.multiply(lab_a, Scalar.all(-0.7071), temp2);
		Core.add(temp1, temp2, lab_b);

//		planes.add(b);
//		planes.add(g);
//		planes.add(r);
		planes.add(lab_l);
		planes.add(lab_a);
		planes.add(lab_b);
		Core.merge(planes, temp);
		return temp.clone();
	}
	
	public void Combine(Mat Salient){
		Imgproc.resize(Salient, Salient, new Size(image.rows(),image.cols()));
		Mat mask = new Mat(image.cols(),image.rows(), CvType.CV_32FC1,Scalar.all(0));
//		int keep = -1;
		Set<Integer> keep = new HashSet<Integer>();
		int label = -1;
		System.out.println("Search Cluster Labels");
		for (int x = 0; x < image.cols();x++){
			for (int y = 0; y < image.rows();y++){
				if (Salient.get(x, y)[0] > 0 ){
					keep.add(clusters[x][y]);
				}
			}
		}
		System.out.println("Combine results");
		for (int x = 0; x < image.cols();x++){
			for (int y = 0; y < image.rows();y++){
				if (keep.contains(clusters[x][y])) mask.put(x, y, 1.0);
			}
		}
		
		Imgproc.threshold(mask, Salient, 0.5, 1.0, Imgproc.THRESH_BINARY);
		
	}
	
	public static void toFile(Mat mat, String file) {
		try {
			FileWriter fw=new FileWriter("/Users/jingweiguo/Documents/OpenCV/results/" + file + ".txt");
			fw.write(mat.toString() + "\n");
			for (int ii=0; ii<mat.rows(); ii++) {
				for (int jj=0; jj<mat.cols(); jj++) {
					fw.write(mat.get(ii, jj)[0] + "	");
				}
				fw.write("\n");
			}
			fw.flush();
			fw.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
class ColorRep{
	float l = 0;
	float a = 0;
	float b = 0;
	float x = 0;
	float y = 0;
    
    ColorRep(){}
    
    ColorRep( float[] color, Point coord ) {
        init( color, (int)coord.x, (int)coord.y );
    }
    
    ColorRep( float[] color, int x, int y ) {
        init( color, x, y );
    }
    
    public void init( float[] color, int x, int y ) {
        this.l = color[0];
        this.a = color[1];
        this.b = color[2];
        this.x = x;
        this.y = y;
    }
    
    void add( float[] color, int x, int y ) {
        this.l += color[0];
        this.a += color[1];
        this.b += color[2];
        this.x += x;
        this.y += y;
    }
    
    void divColor( float divisor ) {
        this.l /= divisor;
        this.a /= divisor;
        this.b /= divisor;
    }
    
    void div( float divisor ) {
        this.l /= divisor;
        this.a /= divisor;
        this.b /= divisor;
        this.x /= divisor;
        this.y /= divisor;
    }
    
	}
}
//    public String toString() {
//        String ss;
//        ss << l << " " << a << " " << b << " " << x << " " << y;
//        return ss.str();
//    }




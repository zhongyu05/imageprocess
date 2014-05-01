package com.example.imageprocess;

import java.util.ArrayList;

import org.opencv.core.*;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.*;
import org.opencv.utils.Converters;

public class SLICSuperpixel {
	protected ArrayList<ArrayList<Integer>> clusters;
	protected ArrayList<ArrayList<Double>> distances;
	protected ArrayList<ColorRep> centers;
	protected ArrayList<Integer> centerCounts;
    
	protected Mat image;
	protected int K;
	protected int S;
	protected int m;
	protected int maxIterations;
    
	protected boolean withinRange( int x, int y ){
		    return x >= 0 && y >= 0 && x < image.cols() && y < image.rows();
	}

	public void clear() {
	    clusters.clear();
	    distances.clear();
	    centers.clear();
	    centerCounts.clear();
	    
	    if( !image.empty() )
	        image.release();
	}
	
	public void generateSuperPixels(){
		
//    	ArrayList<Double> Infiniti = new ArrayList<Double>();
//    	for (int i = 0;i < distances.get(0).size();i++) Infiniti.add(Double.MAX_VALUE);


		/* Repeat until we hit max iterations (or certain threshold in literature) */
	    for( int iter = 0; iter < this.maxIterations; iter++ ) {
	        
	        /* Reset distances */
	        for( ArrayList<Double> dist: distances ){
	        	int dist_size = dist.size();
	        	dist.clear();
	        	for (int i = 0;i < dist_size;i++) dist.add(Double.MAX_VALUE);
//	            dist.assign( dist.size(), Double.MAX_VALUE );
	        }
	        
	        /* For each cluster centers Ck */
	        for( int k = 0; k < centers.size(); k++ ){
	            ColorRep center = centers.get(k);
	            /* For each 2 x Steps region around Ck */
	            for (int y = (int)center.y - S; y <= (int)center.y + S;y++){
	            	 for( int x = (int)center.x - S; x < (int)center.x + S; x++ ){
		                    if( withinRange(x, y) ){
		                    	double[] color = new double[3];
		                    	image.get(y, x, color);
//		                        Vec3b color = ptr[x];
		                        
		                        /* Compute and retain the smaller distance */
		                        double distance = calcDistance( center, color, x, y );
//		                        if( distance < distances[y][x] ) {
		                        if (distance < distances.get(y).get(x) ) {
		                    
		                        	ArrayList<Double> temp_dist = distances.get(y);
		                        	temp_dist.set(x, distance);
//		                            distances[y][x] = distance;
		                        	ArrayList<Integer> temp_cluster = clusters.get(y);
		                        	temp_cluster.set(x, k);
//		                            clusters[y][x]  = k;
		                        }
		                    }
		                }
	            }
	            /*
	            tbb::parallel_for( (int)center.y - S, (int)center.y + S, [&](int y) {
	            	double[] ptrdata = new double[];
	            	
//	                Vec3b * ptr = image.ptr<Vec3b>(y);
	                
	                for( int x = center.x - S; x < center.x + S; x++ ){
	                    if( withinRange(x, y) ){
	                        Vec3b color = ptr[x];
	                        
	                         Compute and retain the smaller distance 
	                        double distance = calcDistance( center, color, x, y );
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
	        for( int y = 0; y < image.rows(); y++ ) {
	            for( int x = 0; x < image.cols(); x++ ) {
//	                int cluster_id = clusters[y][x];
	            	int cluster_id = clusters.get(y).get(x);
	                if( cluster_id > -1 ) {
	                	double[] color = new double[3];
	                	image.get(y, x, color);
//	                    Vec3b color = image.at<Vec3b>(y, x);
	                	ColorRep color_temp = centers.get(cluster_id);
	                	color_temp.add(color, x, y);
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
	    ArrayList<Double> color1 = new ArrayList<Double>(centers.size());
	    ArrayList<Double> color2 = new ArrayList<Double>(centers.size());
	    ArrayList<Double> color3 = new ArrayList<Double>(centers.size());

//	    vector<Vec3f> colors( centers.size() );

	    /* Accumulate the colors for each cluster */
	    for( int y = 0; y < temp.rows(); y++ ) {
//	        Vec3b * ptr = temp.ptr<Vec3b>(y);
	        for( int x = 0; x < temp.cols(); x++ ){
	        	double[] colors = new double[3];
	        	temp.get(y, x, colors);
	        	int index = clusters.get(y).get(x);
	        	color1.set(index, color1.get(index)+colors[0]);
	        	color2.set(index, color2.get(index)+colors[1]);
	        	color3.set(index, color3.get(index)+colors[2]);

	        }
//	            colors[clusters[y][x]] += ptr[x];
	    }

	    /* Get the average of the colors */
	    for( int i = 0; i < color1.size(); i++ ){
	    	color1.set(i, color1.get(i)/centerCounts.get(i));
        	color2.set(i, color2.get(i)/centerCounts.get(i));
        	color3.set(i, color3.get(i)/centerCounts.get(i));
//	        colors[i] /= centerCounts[i];
	    }
	    /* Recolor the original CIELab image with the average color for each clusters */
	    for( int y = 0; y < temp.rows(); y++ ) {
//	        Vec3b * ptr = temp.ptr<Vec3b>(y);
	        for( int x = 0; x < temp.cols(); x++ ) {
	        	int cluster_index = clusters.get(y).get(x);
	        	double[] color = new double[3];
	        	color[0] = color1.get(cluster_index);
	        	color[1] = color2.get(cluster_index);
	        	color[2] = color3.get(cluster_index);
	        	temp.put(y, x, color);
	        	
//	            int cluster_index = clusters[y][x];
//	            Vec3b color = colors[cluster_index];
//	            ptr[x] = Vec3b( color[0], color[1], color[2] );
	        }
	    }
	    
	    return temp;
	}
	
	public ArrayList<Point> getContours() {
	    final int dx[] = { -1, -1, 0, 1, 1, 1, 0, -1 };
	    final int dy[] = { 0, -1, -1, -1, 0, 1, 1, 1 };
	    
	    ArrayList<ArrayList<Boolean>> taken = new ArrayList<ArrayList<Boolean>>();
	    ArrayList<Boolean> all_false = new ArrayList<Boolean>(image.cols());
	    for (int y = 0;y < image.cols();y++) all_false.add(false);
	    
	    for( int y = 0; y < image.rows(); y++ ){
	    	taken.add ( all_false );
	        }
	    
	    ArrayList<Point> contours = new ArrayList<Point>();
	    
	    for( int y = 0; y < image.rows(); y++ ){
	        for( int x = 0; x < image.cols(); x++ ) {
	            int nr_p = 0;
	            
	            for(int k = 0; k < 8; k++ ) {
	                int nx = x + dx[k];
	                int ny = y + dy[k];
	                
	                if( withinRange( nx, ny ) ){
//	                    if( !taken[ny][nx] && clusters[y][x] != clusters[ny][nx] ) {
		                if( !taken.get(ny).get(nx) && clusters.get(y).get(x) != clusters.get(ny).get(nx) ) {

	                    	nr_p++;
	                        
	                        if( nr_p > 1 )
	                            break;
	                    }
	                }
	            }
	            
	            if( nr_p > 1 ) {
	                contours.add( new Point(x, y) );
	                ArrayList<Boolean> temp_taken = taken.get(y);
	                temp_taken.set(x, true);
//	                taken[y][x] = true;
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

	
	public double calcDistance( ColorRep c, double[] p, int x, int y ) {
	    double d_lab = ( (c.l - p[0]) * (c.l - p[0]) + (c.a - p[1]) * (c.a - p[1]) + (c.b - p[2]) * (c.b - p[2]) );
	    double d_xy  = ( (c.x - x) * (c.x - x) + (c.y - y) * (c.y - y)  );
	    return Math.sqrt( d_lab + d_xy / (S * S) * (m * m) );
	}

	
	public Point findLocalMinimum( Mat image, Point center ){
		
		Point minimum = new Point( center.x, center.y );
	    double min_gradient = Double.MAX_VALUE;
	    for( int y = (int)(center.y - 1); y < center.y + 2; y++ ) {
	        for( int x = (int)(center.x - 1); x < center.x + 2; x++ ) {
	        	double[] lab = new double[3];
	        	image.get(y, x, lab);
	        	
	        	double[] lab_dy = new double[3];
	        	image.get(y+1, x, lab_dy);
	        	
	        	double[] lab_dx = new double[3];
	        	image.get(y, x+1, lab_dx);
//	            Vec3b lab    = image.at<Vec3b>( y  , x   );
//	            Vec3b lab_dy = image.at<Vec3b>( y+1, x   );
//	            Vec3b lab_dx = image.at<Vec3b>( y  , x+1 );
	            
	            double diff = Math.abs( lab_dy[0] - lab[0] ) + Math.abs( lab_dx[0] - lab[0] );
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
	    
		Imgproc.cvtColor(src, image, Imgproc.COLOR_BGR2Lab);

//	    Imgproc.cvtColor( src, image, Imgproc.CV_BGR2Lab );
	    
	    /* Initialize cluster centers Ck and move them to the lowest gradient position in 3x3 neighborhood */
	    for( int y = S; y < image.rows() - S / 2; y += S ) {
	        for( int x = S; x < image.cols() - S / 2; x += S ) {
	            Point minimum = findLocalMinimum( image,new Point(x, y));
	            double[] color = new double[3];
	            image.get((int)minimum.y, (int)minimum.x, color);
//	            Vec3b color = image.at<Vec3b>( minimum.y, minimum.x );
	            centers.add(new ColorRep( color, minimum ));
//	            centers.push_back( ColorRep( color, minimum ) );
	        }
	    }

	    /* Set labels to -1 and distances to infinity */
	    ArrayList<Integer> minus_one = new ArrayList<Integer>();
	    ArrayList<Double> Infiniti = new ArrayList<Double>();
	    for (int y = 0; y < image.rows();y++) Infiniti.add(Double.MAX_VALUE);
	    for (int y = 0; y < image.cols();y++) minus_one.add(-1);
	    
	    
	    for( int y = 0; y < image.rows(); y++ ) {
	    	clusters.add(minus_one);
	    	distances.add(Infiniti);
//	        clusters.push_back ( ArrayList<Integer>( image.cols(), -1 ) );
//	        distances.push_back( ArrayList<Double>( image.cols(), Double.MAX_VALUE ) );
	    }
	    
	    centerCounts = new ArrayList<Integer>();
	    for (int y = 0; y < centers.size();y++) centerCounts.add(0);

//	    		( centers.size(), 0 );

	}
	
	public SLICSuperpixel( Mat src, int no_of_superpixels){
	 int m = 10, max_iterations = 10;
	 init( src, no_of_superpixels, m, max_iterations );
	}
	
	public SLICSuperpixel( Mat src, int no_of_superpixels, int m, int max_iterations ){
		 init( src, no_of_superpixels, m, max_iterations );
	}

class ColorRep{
	double l = 0;
	double a = 0;
	double b = 0;
	double x = 0;
	double y = 0;
    
    ColorRep(){}
    
    ColorRep( double[] color, Point coord ) {
        init( color, (int)coord.x, (int)coord.y );
    }
    
    ColorRep( double[] color, int x, int y ) {
        init( color, x, y );
    }
    
    public void init( double[] color, int x, int y ) {
        this.l = color[0];
        this.a = color[1];
        this.b = color[2];
        this.x = x;
        this.y = y;
    }
    
    void add( double[] color, int x, int y ) {
        this.l += color[0];
        this.a += color[1];
        this.b += color[2];
        this.x += x;
        this.y += y;
    }
    
    void divColor( double divisor ) {
        this.l /= divisor;
        this.a /= divisor;
        this.b /= divisor;
    }
    
    void div( double divisor ) {
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


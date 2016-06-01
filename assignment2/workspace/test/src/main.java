import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class main {
	public static final int tMin = 35, tMax = 80; // TODO pas deze waarden aan naar iets logisch
    private final static float ALPHA = 0.25f;
    private final static int M = 4; // Width of moving average
    private final static int S = 2; // Shift of moving average output to compensate for M (determined visually)
	
    private final static int WINDOW_SIZE = 15; 		// Window size in which findPeaks searches for peaks. Should be only slightly larger than period,
    												// Window size should be larger than MIN_INTERPEAK
    private final static int MIN_INTERPEAK = 10;    // Minimum nr of samples between peaks in acc magn values
	
	public static Result findPeaks(List<Double> a) {
		
	    double max = 0;
	    List<Double> peaks = new ArrayList<Double>();
	    List<Integer> indices = new ArrayList<Integer>();
	    
	    int startWindow = 0;
	    int endWindow = startWindow + WINDOW_SIZE;
	    int delta = MIN_INTERPEAK;                      
	    int peakLoc = 0;
	    boolean found = false;
	
	    // Check whole signal for peaks
	    for (int i = 1; i < (a.size() - 1); ) {
	    	
	    	// Find peak in particular time window
	        for (int j = i; j < endWindow; j++) {
	        	
	        	double value = a.get(j);
	        	
	        	// Check if value is a peak
            	if (a.get(j-1) < value && a.get(j+1) < value) {
            		
            		// Check if it is the highest peak in the time window
            		if (value > max) {
            			found = true;
            			max = value;
            			peakLoc = j;
            		}
            	}
	        }
	        
	        // If a peak was found, add to results, else, look in next window
	        if (found) {
		        peaks.add(max);
		        indices.add(peakLoc);
		        found = false;
	        } else {
	        	peakLoc = endWindow - delta;
	        }

	        max = 0;
	
	        if ((peakLoc + delta + WINDOW_SIZE + 1) >= a.size()) {
	        	Result res = new Result(indices, peaks);
	        	return res;
	        }
	
	        i = peakLoc + delta;     // Minimal possible index for new peak
	        endWindow = i + WINDOW_SIZE;   // Maximum possible index for new peak
	

	    }
	    Result res = new Result(indices, peaks); 
	    
	    return res;
	}
	
	public static class Result {
		public List<Integer> xValues;
		public List<Double> yValues;
		
		Result(List<Integer> x, List<Double> y) {
			this.xValues = x;
			this.yValues = y;
		}
		
	}

	/* Read in acc data from file */ 
    public static List<Double> Read(String filename) {

        List<Double> values = new ArrayList<Double>();
        	
	    try {
		    Scanner sc = new Scanner(new File(filename));
	        sc.nextLine();
	        while (sc.hasNextLine())    {
	        	String[] data = sc.nextLine().split("\\|"); 	            	
	        	values.add(Double.parseDouble(data[2]));
	        }
	    } catch (IOException e) {
	    	System.out.println("Data file not found.");
	    }           
	    return values;
    } 
	
    /* Moving average filter */
    private static float[] movingAverage(float[] input, float[] output) {
    	   	
        if (output == null) return input;
        
        for (int i = 0; i < input.length - M; i++) {
        	
        	output[i] = 0;
        	for (int k = M; k >= 0; k--) {
        		output[i] += input[i + M - k];
        	}
        	output[i] = output[i]/M;
        	
        }  
        
        // Shift S elements to the right to match smoothed function with actual function ( this is determined visually)
        for (int i = output.length - 1; i >= S; i--) {
        	output[i] = output[i - S];
        }
        for (int i = 0; i < S; i++) {
        	output[i] = output[S];
        }
        
        return output;
    }
    
    public static void main(String [] args) throws FileNotFoundException, UnsupportedEncodingException
	{
    	List<Double> data = Read("/home/danielle/AndroidStudioProjects/IN4254-SPS/assignment2/accelerometer_DANI.dat");
    	
    	// Dont look at peaks for now, only check smoothing
    	//float[] yOld = new float[data.size()];
    	//float[] yNew = new float[data.size()];
    	float[] yOld = new float[350];
    	float[] yNew = new float[350];
    	
    	//for (int i = 0; i < yOld.length; i++) {
    	for (int i = 0; i < 350; i++) {
    		yOld[i] = data.get(i).floatValue();
    	}
    	
    	yNew = movingAverage(yOld, yNew);

    	data = new ArrayList<Double>();
    	
    	for (int i = 0; i < yNew.length; i++) {
    		
    		data.add(((Float)yNew[i]).doubleValue());
    		
    	}
    	
    	// Check peaks now
    	Result res = findPeaks(data);
    	
    	    	
    	PrintWriter writer = new PrintWriter("smooth.dat", "UTF-8");
    	for (int i = 0; i < 350; i++) {
    		if (res.xValues.contains(i)) {
    			writer.println(yOld[i] + "-" + yNew[i] + "-" + res.yValues.get((res.xValues.indexOf(i))) );
    		} else {
    			writer.println(yOld[i] + "-" + yNew[i] + "-" + 0);
    		}
    	}
    	writer.close();
    	
    	
    	
    	
    	System.out.println(res.xValues);
    	System.out.println(res.yValues);
    	
    	
    	
    }
	
}

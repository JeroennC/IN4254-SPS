package op;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TestAcceleration {

	public static void main(String[] args) {
		
		String[] labels = {"walking", "still", "sudden movements"};
		
		String[] paths = {"C:/Users/jeroe/Documents/Programming/SPS/IN4254-SPS/assignment2/accelData/walking.dat",
				"C:/Users/jeroe/Documents/Programming/SPS/IN4254-SPS/assignment2/accelData/still.dat",
				"C:/Users/jeroe/Documents/Programming/SPS/IN4254-SPS/assignment2/accelData/suddenmovements.dat"};
		for (int i = 0; i < 3; i++) {
			StepDetector stepDetector = new StepDetector();

			LinkedList<Double> dataHistory = new LinkedList<Double>();
			Path p = Paths.get(paths[i]);
			try {
				List<String> lines = Files.readAllLines(p);
				for (String str : lines) {
					dataHistory.add(Double.parseDouble(str));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Put data through
			Iterator<Double> it = dataHistory.iterator();
			int counter = 0;
			int still = 0;
			int walk = 0;
			while (it.hasNext()) {
				double val = it.next();
				stepDetector.handleValue(val);
				if (stepDetector.getState() == StepDetector.State.STILL)
					still++;
				else
					walk++;
			}
			
			System.out.println(labels[i] + ": {Walk: " + walk + ", Still: " + still + "}");
		}
		/*
		double[] f = {-9.96,-3.06,4.14,10.39,13.84,13.44,8.99,1.59,-3.86,-5.36,-2.61,3.84,12.19,17.69,18.79,15.64,9.99,3.84,-1.56,-5.36,-7.11,-7.36,-7.36,-7.71,-8.56,-9.86,-11.61,-13.31,-14.56,-15.26};
		double[] g = {5.62,4.92,4.62,4.62,4.77,4.52,3.87,3.07,2.07,1.02,0.22,-0.23,-0.48,-0.53,-0.43,-0.33,-0.38,-0.63,-1.03,-1.53,-2.13,-2.68,-3.13,-3.48,-3.68,-3.73,-3.78,-3.73,-3.68,-3.63};

		// Remove means
		// Calculate the means and standard deviations
        double fmean = mean(f);
        double gmean = mean(g);
        double fsd = sd(f, fmean);
        double gsd = sd(g, gmean);

        // Don't use STILL data as it gives very bad things!

        // Turn f and g into f' and g' by subtracting the mean
        // NOTE: f and g are equal length
        for (int i = 0; i < f.length; i++) {
            f[i] = f[i] - fmean;
            g[i] = g[i] - gmean;
        }
        
        double val = ac.getAutocorrelation(f, g, fsd, gsd, 0, 0);*/
        
        //System.out.println(val);
	}
	
	/* Compute mean of an array */
    private static double mean (double a[]) {
        double mean = 0;

        for (int i = 0; i < a.length; i++) {
            mean += a[i];
        }
        mean /= a.length;

        return mean;
    }

    /* Compute standard deviation of an array */
    private static double sd (double a[], double mean){
        double sum = 0;

        // Compute stdev
        for (int i = 0; i < a.length; i++)
            sum += Math.pow((a[i] - mean), 2);

        return Math.sqrt( sum / a.length );
    }

}
package op;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class AutoCorrelation {
	private int offsetL, offsetR;

	public AutoCorrelation() {
		offsetL = 50;
		offsetR = 50;
	}
	
	public void processData(LinkedList<Double> magnitudeData) {
		// Find the last two peaks in data
		int peak1 = 50;
		int peak2 = 150;
		// Separate out f and g
		double f[] = new double[100];
		double g[] = new double[100];
		// Get starting and ending indices
		int fstart = peak1 - offsetL < 0 ? 0 : peak1 - offsetL;
		int fend = peak1 + offsetR > magnitudeData.size() ? magnitudeData.size() : peak1 + offsetR;
		int gstart = peak2 - offsetL < 0 ? 0 : peak2 - offsetL;
		int gend = peak2 + offsetR > magnitudeData.size() ? magnitudeData.size() : peak2 + offsetR;

		// Iterate through the list and extract values to f and g 
		Iterator<Double> it = magnitudeData.listIterator(fstart);
		int index = fstart;
		while(it.hasNext()) {
			double val = it.next();
			if (index < fend)
				f[index - fstart] = val;
			if (index >= gstart && index < gend)
				g[index - gstart] = val;
			index++;
		}
		
		// Calculate the means and standard deviations
		double fmean = mean(f);
		double gmean = mean(g);
		double fsd = sd(f, fmean);
		double gsd = sd(g, gmean);
		
		// Turn f and g into f' and g' by subtracting the mean
		// NOTE: f and g are equal length
		for (int i = 0; i < f.length; i++) {
			f[i] = f[i] - fmean;
			g[i] = g[i] - gmean;
		}
		
		// Find the optimal autocorrelation
		double opt = getOptimalAutocorrelation(f,g);
		
		System.out.printf("The optimal: %.2f", opt);
	}
	
	/* Shift g over f and try to find the optimal autocorrelation */
	private double getOptimalAutocorrelation(double f[], double g[]) {
		double optimalCorrelation = Double.MIN_VALUE;
		double val;
		for (int i = 0; i < g.length * 2 - 1; i++) {
			val = getAutocorrelation(f, g
					,-g.length + i + 1 < 0 ? 0 :-g.length + i + 1
					, g.length - i - 1 < 0 ? 0 : g.length - i - 1);
			if (val > optimalCorrelation) {
				optimalCorrelation = val;
				// TODO store the corresponding variables
			}
		}
		
		return optimalCorrelation;
	}
	
	/* Calculate autocorrelation with f and g, with gi and fi above eachother. Either gi or fi should alwayss be 0 */
	private double getAutocorrelation(double f[], double g[], int fi, int gi) {
		double result = 0;
		int overlap = fi == 0 ? g.length - gi : f.length - fi;
		
		for (int i = 0; i < overlap; i++) {
			result += f[fi + i] * g[gi + i];
		}
		
		return result;
	}
	
	/* Compute mean of an array */
    private double mean (double a[]) {
        double mean = 0;

        for (int i = 0; i < a.length; i++) {
            mean += a[i];
        }
        mean /= a.length;

        return mean;
    }
    
    /* Compute standard deviation of an array */
    private double sd (double a[], double mean){
        double sum = 0;

        // Compute stdev
        for (int i = 0; i < a.length; i++)
            sum += Math.pow((a[i] - mean), 2);

        return Math.sqrt( sum / a.length );
    }
	
	public static void main(String[] args) {
		AutoCorrelation ac = new AutoCorrelation();
		LinkedList<Double> dataHistory = new LinkedList<Double>();
		
		Path p = Paths.get("C:/Users/jeroe/Documents/Programming/SPS/IN4254-SPS/assignment2/accelerometer_DANI.dat");
		try {
			List<String> lines = Files.readAllLines(p);
			lines.remove(0);
			for (String str : lines) {
				String[] parts = str.split("\\|");
				System.out.println(parts[0] + parts[1] + parts[2]);
				dataHistory.add(Double.parseDouble(parts[2]));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ac.processData(dataHistory);
		
	}

}

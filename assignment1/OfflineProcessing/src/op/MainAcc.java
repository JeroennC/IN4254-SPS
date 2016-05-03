package op;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads accelerator data and access point data
 * Converts
 */
public class MainAcc {
	
	private class AccData {
		long timestamp;
		double x, y, z;
		double tot;
		
		AccData(long timestamp, double x, double y, double z) {
			this.timestamp = timestamp;
			this.x = x;
			this.y = y;
			this.z = z;
			tot = Math.abs(x) + Math.abs(y) + Math.abs(z);
		}
	}



	public MainAcc() {

	}


	public void start() {
		// Read acc data
		List<AccData> accDataWalk = readAcc("walk.dat");
		List<AccData> accDataStill = readAcc("still.dat");

		// Test multiple timewindows? (milliseconds)
		long bestWindow = -1;
		double bestAccuracy = -1;
		for (long timeWindow = 100; timeWindow < 1000; timeWindow += 100) {
			// Reset classifier
			
			// Extract features
			List<Feature> featuresWalk = getAccFeatures(accDataWalk, timeWindow, "walk");
			List<Feature> featuresStill = getAccFeatures(accDataStill, timeWindow, "still");
			List<Feature> features = new ArrayList<Feature>(featuresWalk);
			features.addAll(featuresStill);
			
			// Get a training set and test set
			List<Feature> training = new ArrayList<Feature>();
			List<Feature> test = new ArrayList<Feature>();
			Classifier.SplitFeatureSet(features, training, test, 0.1);

			// Train classifier, using k = 7 for the kNN method
			Classifier classifier = new Classifier(7);
			classifier.trainClassifier(training);

			// Test classifier
			double accuracy = classifier.testData(test);
			
			System.out.println("Classification accuracy: " + accuracy);
			
			if (accuracy > bestAccuracy) {
				bestAccuracy = accuracy;
				bestWindow = timeWindow;
			}
		}
		
		System.out.println("Accuracy: " + bestAccuracy);
		System.out.println("Best window: " + bestWindow);
		
		// Output best window to file
		bestWindow = 600;
		List<Feature> featuresWalk = getAccFeatures(accDataWalk, bestWindow, "walk");
		List<Feature> featuresStill = getAccFeatures(accDataStill, bestWindow, "still");
		List<Feature> features = new ArrayList<Feature>(featuresWalk);
		features.addAll(featuresStill);
		
		Path output = Paths.get("./data/accelerator.dat");
		String line;
		String content = "";
		for (Feature f : features) {
			line = f.value + "|" + f.label;
			content += line + "\n";
		}
		try {
			Files.write(output, content.getBytes(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private List<AccData> readAcc(String filename) {
		List<String> lines;
		try {
			Path filePath = Paths.get("./data/" + filename);
			//Path filePath = Paths.get(filename);
			lines = Files.readAllLines(filePath);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		List<AccData> data = new ArrayList<AccData>();
		// Read lines
		long timestamp;
		double x,y,z;
		String[] inf;
		for (String line : lines) {
			inf = line.split("\\|");
			timestamp = Long.parseLong(inf[0]);
			x = Double.parseDouble(inf[1]);
			y = Double.parseDouble(inf[2]);
			z = Double.parseDouble(inf[3]);
			data.add(new AccData(timestamp, x, y, z));
		}
		
		return data;
	}
	
	/**
	 * Get min-max feature for every time window
	 */
	private List<Feature> getAccFeatures(List<AccData> data, long timeWindow, String label) {
		List<Feature> result = new ArrayList<Feature>();
		if (data.isEmpty()) return result;
		
		AccData ad = data.get(0);
		long nextTimestamp = ad.timestamp + timeWindow;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		// Extract min-max feature from data
		for (int i = 0; i < data.size(); i++) {
			ad = data.get(i);
			
			while (ad.timestamp > nextTimestamp) {
				nextTimestamp += timeWindow;
				if (min == Double.MAX_VALUE)
					continue;
				// reset
				result.add(new Feature(max - min, label));
				min = Double.MAX_VALUE;
				max = Double.MIN_VALUE;
			}
			
			if (ad.tot < min) min = ad.tot;
			if (ad.tot > max) max = ad.tot;
		}
		// Ignore the last time window? (Which is not used now)
		
		return result;		
	}
	
	public static void main(String[] args) {
		new MainAcc().start();
	}
}

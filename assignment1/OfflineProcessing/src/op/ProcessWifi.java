package op;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProcessWifi {

	public static void main(String[] args) {
		
		class DataPoint {
			String bssid;
			String strength;
			
			DataPoint(String bssid, String strength) {
				this.bssid = bssid;
				this.strength = strength;
			}
		}
		
		class SamplePoint {
			String cell;
			List<DataPoint> data;
			
			SamplePoint(String cell) {
				this.cell = cell;
				data = new ArrayList<DataPoint>();
			}
			
			void addPoint(DataPoint p) {
				data.add(p);
			}
		}
		List<String> uniques = new ArrayList<String>();
		List<SamplePoint> measurements = new ArrayList<SamplePoint>();
		List<String> BSSIDS = new ArrayList<String>();
		Path p = Paths.get("C:/Users/jeroe/Desktop/novaMagnetoWifi.dat");	
		
		try {
			List<String> lines = Files.readAllLines(p);
			String lastcell = "-100";
			SamplePoint sp = null;
			for (String str : lines) {
				if (str.startsWith("Opened"))
					continue;
				String[] parts = str.split("\t");
				String cell = parts[1];
				String bssid = parts[2];
				String strength = parts[3];
				if (Integer.parseInt(cell) <= 7)
					continue;

				if (!lastcell.equals(cell) || BSSIDS.contains(bssid)) {
					// Next thingy
					if (sp != null)
						measurements.add(sp);
					
					BSSIDS.clear();
					sp = new SamplePoint(cell);
					lastcell = cell;
				}
				
				if (!uniques.contains(bssid))
					uniques.add(bssid);
				BSSIDS.add(bssid);
				sp.addPoint(new DataPoint(bssid, strength));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (String str: uniques)
			System.out.println(str);
		
		if (true)
			return;
		// Print
		for (SamplePoint sp : measurements) {
			String output = "{ \"cell\": " + sp.cell + ", \"data\": [";
			
			boolean first = true;
			for (DataPoint dp : sp.data) {
				if (!first) {
					output += ",";
				}
				output += "{\"bssid\": \"" + dp.bssid + "\", \"strength\": " + dp.strength + "}";
				first = false;
				
			}
			
			output += " ]},";
			System.out.println(output);
		}
		
	}

}

package op;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainWifi {
	
	private class WifiFeatures {
		Map<String, List<Feature>> data;
		
		WifiFeatures() {
			data = new HashMap<>();
		}
		
		void addValue(String ap_mac, Feature feat) {
			// Check if list is already present
			List<Feature> ap_data = data.get(ap_mac);
			if (ap_data == null) {
				ap_data = new ArrayList<Feature>();
				data.put(ap_mac, ap_data);
			}
			// Add value
			ap_data.add(feat);
		}
	}
	
	private class WifiCollection {
		Map<String, List<WifiData>> data;
		
		WifiCollection() {
			data = new HashMap<>();
		}
		
		void addValue(String ap_mac, WifiData new_data) {
			// Check if list is already present
			List<WifiData> ap_data = data.get(ap_mac);
			if (ap_data == null) {
				ap_data = new ArrayList<WifiData>();
				data.put(ap_mac, ap_data);
			}

			// Check if timestamp is already present
			for (WifiData wd : ap_data) {
				if (wd.timestamp == new_data.timestamp)
					return;
			}
			
			// Add value
			ap_data.add(new_data);
		}
		
		void getFeatures(WifiFeatures output, String label) {
			for (Map.Entry<String, List<WifiData>> pair : data.entrySet()) {
				for (WifiData wd : pair.getValue()) {
					Feature feat = new Feature(wd.level, label);
					output.addValue(pair.getKey(), feat);
				}
			}
		}
	}
	
	private class WifiData {
		long timestamp;
		int level;
		
		WifiData(long timestamp, int level) {
			this.timestamp = timestamp;
			this.level = level;
		}
	}

	public void start() {
		// Read data
		WifiCollection dataRoomA = readWifi("roomA.dat");
		WifiCollection dataRoomB = readWifi("roomB.dat");
		WifiFeatures features = new WifiFeatures();
		dataRoomA.getFeatures(features, "roomA");
		dataRoomB.getFeatures(features, "roomB");
		
		// Output features
		Path output = Paths.get("./data/wifi.dat");
		String line;
		String content = "";
		for (Map.Entry<String, List<Feature>> pair : features.data.entrySet()) {
			for (Feature f : pair.getValue()) {
				line = pair.getKey() + "|" + f.value + "|" + f.label;
				content += line + "\n";
			}
		}

		try {
			Files.write(output, content.getBytes(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private WifiCollection readWifi(String filename) {
		List<String> lines;
		try {
			Path filePath = Paths.get("./data/" + filename);
			//Path filePath = Paths.get(filename);
			lines = Files.readAllLines(filePath);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		WifiCollection data = new WifiCollection();
		
		// Read lines
		long timestamp;
		String ap_mac;
		int level;
		String[] inf;
		for (String line : lines) {
			inf = line.split("\\|");
			timestamp = Long.parseLong(inf[0]);
			ap_mac = inf[1];
			level = Integer.parseInt(inf[2]);
			data.addValue(ap_mac, new WifiData(timestamp, level));
		}
		
		return data;
	}
	
	public static void main(String[] args) {
		new MainWifi().start();
	}

}

package dataVisualizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class Parser {
	final int AGGR_DATA_COUNT = 20;
	// List of zipped csvs
	ArrayList<String> csvs;

	// Takes root folder for csvs
	public Parser(String dir) {
		this.csvs = new ArrayList<String>();
		findCsvs(new File(dir));
	}

	// get list of zipped csvs under the given directory
	private void findCsvs(File dir) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				findCsvs(file);
			} else if (file.getName().endsWith(".csv.gz")) {
				this.csvs.add(file.getAbsolutePath());
			}
		}
	}

	// parses zipped csv and returns a list of data
	@SuppressWarnings("resource")
	private ArrayList<Data> parseGz(String file) {
		ArrayList<Data> data = new ArrayList<Data>();
		ArrayList<String> rowAcc = new ArrayList<String>();
		String type = file.contains("Phone") ? "phone" : "watch";
		
		try {
			GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(file));
			InputStreamReader reader = new InputStreamReader(gzis);
			BufferedReader in = new BufferedReader(reader);

			String nextLine = in.readLine(); // ignore header
			if(nextLine == null || !nextLine.equals("HEADER_TIME_STAMP,X_ACCELATION_METERS_PER_SECOND_SQUARED," +
					"Y_ACCELATION_METERS_PER_SECOND_SQUARED,Z_ACCELATION_METERS_PER_SECOND_SQUARED")){
				return new ArrayList<Data>();
			}
			while ((nextLine = in.readLine()) != null) {
				rowAcc.add(nextLine);
				if(rowAcc.size() >= AGGR_DATA_COUNT) {
					data.add(new Data(type, rowAcc));
					rowAcc.clear();
				}
			}
			if(rowAcc.size() != 0) {
				Data d = new Data(type, rowAcc);
				if(d.isCorrupt) {
					data.add(d);
				}
			}

			gzis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return data;
	}
	
	public ArrayList<Data> getPhoneData() {
		ArrayList<Data> dataAcc = new ArrayList<Data>();
		for(int i = 0; i < csvs.size(); i++) {
			if(csvs.get(i).contains("Phone")) {
				dataAcc.addAll(parseGz(csvs.get(i)));
			}
		}
		return dataAcc;
	}
	
	public ArrayList<Data> getWatchData() {
		ArrayList<Data> dataAcc = new ArrayList<Data>();
		for(int i = 0; i < csvs.size(); i++) {
			if(csvs.get(i).contains("GWatch")) {
				dataAcc.addAll(parseGz(csvs.get(i)));
			}
		}
		return dataAcc;
	}
}

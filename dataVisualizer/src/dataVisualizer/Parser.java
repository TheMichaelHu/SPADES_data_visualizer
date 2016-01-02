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
	String[] start;
	String[] end;

	// Takes root folder for csvs
	public Parser(String dir) {
		this.csvs = new ArrayList<String>();
		findCsvs(new File(dir));
	}
	
	public Parser(String dir, String start, String end) {
		if (!start.matches("([0-9]{2})/([0-9]{2})/([0-9]{4})(/([0-9]{2}))?")
				|| !end.matches("([0-9]{2})/([0-9]{2})/([0-9]{4})(/([0-9]{2}))?")) {
			throw new RuntimeException("Date format.");
		}
		this.csvs = new ArrayList<String>();
		this.start = start.split("/");
		this.end = end.split("/");
		findCsvs(new File(dir));
	}

	// get list of zipped csvs under the given directory
	private void findCsvs(File dir) {
		if(this.start != null && this.end != null) {
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					findCsvs(file);
				} else if (file.getName().endsWith(".csv.gz") && csvDateCheck(file)) {
					this.csvs.add(file.getAbsolutePath());
				}
			}
		} else {
			for (File file : dir.listFiles()) {
				if (file.isDirectory()) {
					findCsvs(file);
				} else if (file.getName().endsWith(".csv.gz")) {
					this.csvs.add(file.getAbsolutePath());
				}
			}
		}
	}

	// return if the csv contains data between the given start and end dates
	// Dates should be formatted as "mm/dd/yyyy"
	private boolean csvDateCheck(File file) {
		String[] path = file.getAbsolutePath().split(File.separator);
		String[] csvDate = new String[4];
		csvDate[1] = path[path.length - 3];
		csvDate[0] = path[path.length - 4];
		csvDate[2] = path[path.length - 5];
		csvDate[3] = path[path.length - 2]; // optional hour
		
		return this.compareDates(csvDate, start) >= 0
				&& this.compareDates(csvDate, end) <= 0;
	}

	private int compareDates(String[] date1, String[] date2) {
		if (!date1[2].equals(date2[2])) {
			return Integer.parseInt(date1[2]) - Integer.parseInt(date2[2]);
		} else if (!date1[0].equals(date2[0])) {
			return Integer.parseInt(date1[0]) - Integer.parseInt(date2[0]);
		} else if (!date1[1].equals(date2[1])) {
			return Integer.parseInt(date1[1]) - Integer.parseInt(date2[1]);
		} else if (date1.length > 3 && date2.length > 3) {
			return Integer.parseInt(date1[3]) - Integer.parseInt(date2[3]);
		}
		return 0;
	}

	// parses zipped csv and returns a list of data
	@SuppressWarnings("resource")
	private ArrayList<Data> parseGz(String file) {
		ArrayList<Data> data = new ArrayList<Data>();
		ArrayList<String> rowAcc = new ArrayList<String>();
		String type = file.contains("Phone") ? "phone" : "watch";

		try {
			GZIPInputStream gzis = new GZIPInputStream(
					new FileInputStream(file));
			InputStreamReader reader = new InputStreamReader(gzis);
			BufferedReader in = new BufferedReader(reader);

			String nextLine = in.readLine(); // ignore header
			if (nextLine == null
					|| !nextLine
							.equals("HEADER_TIME_STAMP,X_ACCELATION_METERS_PER_SECOND_SQUARED,"
									+ "Y_ACCELATION_METERS_PER_SECOND_SQUARED,Z_ACCELATION_METERS_PER_SECOND_SQUARED")) {
				return new ArrayList<Data>();
			}
			while ((nextLine = in.readLine()) != null) {
				rowAcc.add(nextLine);
				if (rowAcc.size() >= AGGR_DATA_COUNT) {
					data.add(new Data(type, rowAcc));
					rowAcc.clear();
				}
			}
			if (rowAcc.size() != 0) {
				Data d = new Data(type, rowAcc);
				if (d.isCorrupt) {
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
		for (int i = 0; i < csvs.size(); i++) {
			if (csvs.get(i).contains("Phone")) {
				dataAcc.addAll(parseGz(csvs.get(i)));
			}
		}
		return dataAcc;
	}

	public ArrayList<Data> getWatchData() {
		ArrayList<Data> dataAcc = new ArrayList<Data>();
		for (int i = 0; i < csvs.size(); i++) {
			if (csvs.get(i).contains("GWatch")) {
				dataAcc.addAll(parseGz(csvs.get(i)));
			}
		}
		return dataAcc;
	}
}

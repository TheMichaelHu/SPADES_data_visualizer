package dataVisualizer;

import org.apache.commons.cli.*;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class Visualizer {
	static ArrayList<Day> data = new ArrayList<>(), dataW = new ArrayList<>(), battery = new ArrayList<>(),
			batteryW = new ArrayList<>();
	static HashMap<String, ArrayList<Day>> sensorData = new HashMap<>();
	static ArrayList<Prompt> prompts = new ArrayList<>();
	static ArrayList<Event> annotations = new ArrayList<>(), calls = new ArrayList<>(), texts = new ArrayList<>(),
			sessions = new ArrayList<>();
	static HashMap<String, String> sensorLocations = new HashMap<>();

	public static void main(String[] args) throws IOException, ParseException {
		Options options = new Options();
		options.addOption("i", "input-dir", true, "input directory");
		options.addOption("o", "output-dir", true, "output directory");
		Option option = new Option("s", "sensors", true, "sensors to display");
		option.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(option);
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse( options, args);
			String inputDir = cmd.getOptionValue("i");
			String outputDir = cmd.getOptionValue("o");
			String[] sensors = cmd.getOptionValues("s");

			if(inputDir != null) {
				Utils.HOME_DIR = inputDir;
			}
			if(outputDir != null) {
				Utils.TARGET_DIR = outputDir;
			}
			if(sensors != null) {
				Utils.SENSORS = sensors;
			}

		} catch (org.apache.commons.cli.ParseException e) {
			e.printStackTrace();
		}
		validateInputs();
		Utils.updateDirs();
		populateSensorLocations();

		ArrayList<String> monthPlots = parseAndDrawMonths();

		Drawer graph = new Drawer();
		String fileName = String.format("%s%s.%s", Utils.TARGET_DIR, Utils.TITLE, Utils.IMAGE_EXT);
		if(graph.exportFullPlot(monthPlots, fileName)) {
			for (String path : monthPlots) {
				//noinspection ResultOfMethodCallIgnored
				new File(path).delete();
			}
		} else {
			System.err.println("Couldn't draw data.");
		}
		System.out.println("Done!");
	}

	private static void validateInputs() {
		File home = new File(Utils.HOME_DIR);
		if (home.isDirectory()) {
			if (Utils.HOME_DIR.toCharArray()[Utils.HOME_DIR.length() - 1] != '/') {
				Utils.HOME_DIR += "/";
			}
		} else {
			throw new RuntimeException(Utils.HOME_DIR + " is not a directory");
		}

		File target = new File(Utils.TARGET_DIR);
		if (target.isDirectory()) {
			if (Utils.TARGET_DIR.toCharArray()[Utils.TARGET_DIR.length() - 1] != '/') {
				Utils.TARGET_DIR += "/";
			}
		} else {
			throw new RuntimeException(Utils.TARGET_DIR + " is not a directory");
		}
	}

	private static void populateSensorLocations() {
		try {
			File csv = new File(Utils.SENSOR_FILE);
			if(!csv.exists()) {
				System.err.println("missing sensors file " + Utils.SENSOR_FILE);
				return;
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(csv.getPath()), "UTF-8"));
			String nextLine = in.readLine();
			if (nextLine == null
					|| !nextLine
					.equals("SENSOR_ID,LOCATION")) {
				return;
			}
			while ((nextLine = in.readLine()) != null) {
				String[] row = nextLine.split(",");
				if(row.length > 1) {
					sensorLocations.put(row[0], row[1]);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// If drawing by months, we'll save every months worth of data and export it to an image to be
	// aggregated later. If drawing by weeks, all data will be saved at once and nothing will be exported.
	private static ArrayList<String> parseAndDrawMonths() throws IOException, ParseException {
		File folder = new File(Utils.DATA_DIR);
		File[] listOfYears = folder.listFiles();
		ArrayList<String> monthPlots = new ArrayList<>();

		handleEventsAndAnnotations();
		Visualizer.prompts = getPrompts();
		Visualizer.sessions = getSessions();

		if(listOfYears == null) {
			return monthPlots;
		}

		for(File year : listOfYears) {
			File[] listOfMonths = year.listFiles();
			if (listOfMonths == null) {
				continue;
			}

			for(int i = 0; i < listOfMonths.length/2; i++) {
				File temp;
				temp = listOfMonths[i];
				listOfMonths[i] = listOfMonths[listOfMonths.length-i-1];
				listOfMonths[listOfMonths.length-i-1] = temp;
			}

			int monthCount = listOfMonths.length;
			int count = 1;

			for (File month : listOfMonths) {
				if (!month.getName().matches("[01][0-9]")) {
					continue;
				}
				gatherData(month, year);
				String fileName = String.format("%s%s_%s_%s.%s",
						Utils.TARGET_DIR, Utils.TITLE, month.getName(), year.getName(), Utils.IMAGE_EXT);

				if(Utils.CHART_TYPE == Utils.ChartType.byDay) {
					monthPlots.add(fileName);
					drawData(fileName);
					clearData();
				}
				System.out.println(String.format("Finished month %d of %d in %s", count++, monthCount, year.getName()));
			}
		}
		return monthPlots;
	}

	private static void gatherData(File month, File year) {
		Thread getPhoneDataThread = new Thread(new DataRunner(new PhoneDataGetter(month, year)));
		getPhoneDataThread.start();

		Thread getWatchDataThread = new Thread(new DataRunner(new WatchDataGetter(month, year)));
		getWatchDataThread.start();

		Thread getPhoneBatteryDataThread = new Thread(new DataRunner(new PhoneBatteryDataGetter(month, year)));
		getPhoneBatteryDataThread.start();

		Thread getWatchBatteryDataThread = new Thread(new DataRunner(new WatchBatteryDataGetter(month, year)));
		getWatchBatteryDataThread.start();

		ArrayList<Thread> getSensorDataThreads = new ArrayList<>();
		Set<String> sensorIds = sensorLocations.keySet();
		for(String sensorId : sensorIds) {
			Thread getSensorDataThread = new Thread(new DataRunner(new SensorDataGetter(month, year, sensorId)));
			getSensorDataThread.start();
			getSensorDataThreads.add(getSensorDataThread);
		}

		try {
			getPhoneDataThread.join();
			getWatchDataThread.join();
			getPhoneBatteryDataThread.join();
			getWatchBatteryDataThread.join();

			for(Thread thread : getSensorDataThreads) {
				thread.join();
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void drawData(String fileName) throws IOException {
		Drawer graph = new Drawer();
		graph.exportImage(fileName);
	}

	private static void clearData() {
		data.clear();
		dataW.clear();
		battery.clear();
		batteryW.clear();
		for(String sensorId : sensorLocations.keySet()) {
			sensorData.get(sensorId).clear();
		}
	}

	private static void handleEventsAndAnnotations() throws IOException, ParseException {
		ArrayList<Event> annotations = new ArrayList<>(), calls = new ArrayList<>(), texts = new ArrayList<>();
		File folder = new File(Utils.DATA_DIR);
		if(!folder.exists()) {
			System.err.println("missing data dir " + Utils.DATA_DIR);
			return;
		}
		File[] listOfYears = folder.listFiles();

		if(listOfYears == null)
			return;

		for(File year : listOfYears) {

			File[] listOfMonths = year.listFiles();
			if(listOfMonths == null) {
				continue;
			}

			for(File month : listOfMonths) {
				for (int j = 1; j <= 31; j++) {
					for (int i = 0; i < 24; i++) {
						String DAY = ((Integer.toString(j).length() < 2) ? ("0" + Integer.toString(j)) :
								(Integer.toString(j)));

						String HOUR = ((Integer.toString(i).length() < 2) ? ("0" + Integer.toString(i))
								: (Integer.toString(i)));

						File temp = new File(month.getPath() + "/" + DAY + "/" + HOUR);
						File[] listOfTemps = temp.listFiles();
						ArrayList<File> annotationFiles = new ArrayList<>();
						if (listOfTemps == null) {
							continue;
						}

						for (File alpha : listOfTemps) {
							if (alpha.getName().contains("annotation") &&
									alpha.getName().toLowerCase().endsWith("csv.gz") &&
									alpha.isFile()) {
								annotationFiles.add(alpha);
							}

							if(alpha.getName().toLowerCase().contains("phonecall") &&
									alpha.getName().toLowerCase().contains("event") &&
									alpha.getName().toLowerCase().endsWith("csv.gz") && alpha.isFile()) {
								calls.addAll(getPhoneEvent(alpha));
							}

							if(alpha.getName().toLowerCase().contains("phonesms") &&
									alpha.getName().toLowerCase().contains("event") &&
									alpha.getName().toLowerCase().endsWith("csv.gz") && alpha.isFile()) {
								texts.addAll(getPhoneEvent(alpha));
							}
						}

						annotations.addAll(getAnnotations(annotationFiles));
					}
				}
			}
		}

		Visualizer.annotations = annotations;
		Visualizer.calls = calls;
		Visualizer.texts = texts;
	}

	private static ArrayList<Prompt> getPrompts() throws IOException, ParseException {

		ArrayList<Prompt> ret = new ArrayList<>();

		File folder = new File(Utils.SURVEY_DIR);
		if(!folder.exists()) {
			System.err.println("missing survey dir " + Utils.SURVEY_DIR);
			return new ArrayList<>();
		}
		File[] listOfFiles = folder.listFiles();
		if(listOfFiles == null) {
			return ret;
		}

		for (File dir : listOfFiles) {
			InputStream csv = null, csvR = null;
			File[] files = dir.listFiles();
			if (files == null) {
				continue;
			}
			for (File alpha : files) {

				if (alpha.getName().toLowerCase().contains("prompts.csv") && alpha.isFile()) {

					csv = new FileInputStream(alpha.getPath());
				}

				if (alpha.getName().toLowerCase().contains("prompt") &&
						alpha.getName().toLowerCase().contains("responses") &&
						alpha.getName().toLowerCase().endsWith("csv") && alpha.isFile()) {

					csvR = new FileInputStream(alpha.getPath());
				}
			}

			if(csv == null || csvR == null)
				continue;

			Reader decoder = new InputStreamReader(csv, "UTF-8"), decoderR = new InputStreamReader(csvR, "UTF-8");
			BufferedReader buffered = new BufferedReader(decoder), bufferedR = new BufferedReader(decoderR);
			buffered.readLine();
			bufferedR.readLine();

			String text = buffered.readLine();
			String textR = bufferedR.readLine();
			String[] row;

			while (text != null && textR != null && (row = text.split(",")).length > 6) {
				DateFormat df = new SimpleDateFormat("\"y-M-d k:m:s\"", Locale.ENGLISH);
				String[] rowR = textR.split(",");

				ret.add(new Prompt(df.parse(row[0]).getTime(), row[6].contains("Never"),
						rowR.length > 11 ? rowR[11] : "",
						rowR.length > 8 ? rowR[8] : ""));

				text = buffered.readLine();
				textR = bufferedR.readLine();

			}
			buffered.close();
			bufferedR.close();
		}
		return ret;

	}

	private static ArrayList<Event> getAnnotations(ArrayList<File> annotationFiles) throws IOException, ParseException {
		ArrayList<Event> ret = new ArrayList<>();
		InputStream gz;

		for (File file : annotationFiles) {
			if (file == null) {
				continue;
			}
			gz = new GZIPInputStream(new FileInputStream(file.getPath()));

			Reader decoder = new InputStreamReader(gz, "UTF-8");
			BufferedReader buffered = new BufferedReader(decoder);
			buffered.readLine();
			String text = buffered.readLine();
			String[] row;

			while (text != null && (row = text.split(",")).length <= 8) {
				DateFormat df = new SimpleDateFormat("y-M-d k:m:s.S", Locale.ENGLISH);
				ret.add(new Event(df.parse(row[1]).getTime(), df.parse(row[2]).getTime(), row[3]));
				text = buffered.readLine();
			}
			buffered.close();
		}
		return ret;
	}

	private static ArrayList<Event> getPhoneEvent(File file) throws IOException, ParseException {
		ArrayList<Event> ret = new ArrayList<>();
		InputStream gz = new GZIPInputStream(new FileInputStream(file.getPath()));
		Reader decoder = new InputStreamReader(gz, "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);
		buffered.readLine();
		String text;

		while ((text = buffered.readLine()) != null) {
			DateFormat df = new SimpleDateFormat("y-M-d k:m:s.S", Locale.ENGLISH);
			String[] row = text.split(",");
			if(row.length > 4) {
				try {
					ret.add(new Event(df.parse(row[1]).getTime(), df.parse(row[2]).getTime(), row[4]));
				} catch (ParseException e) {
					ret.add(new Event(df.parse(row[1]).getTime(), null, row[4]));
				}
			}
		}

		buffered.close();

		return ret;
	}

	private static ArrayList<Event> getSessions() throws IOException, ParseException {
		ArrayList<Event> ret = new ArrayList<>();
		File file = new File(Utils.SESSION_FILE);
		File[] files = new File(Utils.ROOT_DIR).listFiles();
		if(files != null) {
			for (File f : files) {
				if (f.exists() && f.getName().contains("Session")) {
					file = f;
					break;
				}
			}
			if (!file.exists()) {
				System.err.println("missing sessions file " + Utils.SESSION_FILE);
				return ret;
			}
		} else {
			System.err.println("missing root dir " + Utils.ROOT_DIR);
			return ret;
		}

		Reader decoder = new InputStreamReader(new FileInputStream(file.getPath()), "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);
		buffered.readLine();

		String text;
		String[] row;

		while ((text = buffered.readLine()) != null && (row = text.split(",")).length > 3) {
			DateFormat df = new SimpleDateFormat("M/d/y k:m", Locale.ENGLISH);
			try {
				ret.add(new Event(df.parse(row[2]).getTime(), df.parse(row[3]).getTime(), row[1]));
			} catch(ParseException e) {
				// do nothing
			}
		}
		buffered.close();
		return ret;
	}
}

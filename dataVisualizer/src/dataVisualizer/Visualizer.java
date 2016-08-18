package dataVisualizer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class Visualizer {
	static ArrayList<Day> data = new ArrayList<>(), dataW = new ArrayList<>(), battery = new ArrayList<>(),
			batteryW = new ArrayList<>();
	static HashMap<String, ArrayList<Day>> sensorData = new HashMap<>();
	static HashMap<String, LegendItem> colors = new HashMap<>();
	static ArrayList<Prompt> prompts = new ArrayList<>();
	static ArrayList<Event> annotations = new ArrayList<>(), calls = new ArrayList<>(), texts = new ArrayList<>(),
			sessions = new ArrayList<>();
	static HashMap<String, String> sensorLocations = new HashMap<>();

	public static void main(String[] args) throws IOException {
		try {
//			Utils.HOME_DIR = args[0];
//			Utils.TARGET_DIR = args[1];

			Utils.HOME_DIR = "/home/michael/Desktop/SPADES_8/data/SPADES_8";
			Utils.TARGET_DIR = "/home/michael/Desktop/";

			// Do checks on inputs plz

		} catch(IndexOutOfBoundsException e) {
			System.err.print("Need 2 args: data dir (should contain mastersynced) and target dir");
			throw e;
		}
		Utils.updateDirs();
		populateSensorLocations();
		populateColors();

		parse();

		Drawer graph = new Drawer();
		File target = new File(Utils.TARGET_DIR);
		if (target.isDirectory()) {
			if (Utils.TARGET_DIR.toCharArray()[Utils.TARGET_DIR.length() - 1] != '/') {
				Utils.TARGET_DIR += "/";
			}
			ImageIO.write(graph.getImages(), "PNG", new File(Utils.TARGET_DIR + Utils.TITLE + ".png"));
		} else {
			System.err.println(Utils.TARGET_DIR + " is not a directory");
		}

		graph.drawInFrame();
	}

	private static void populateSensorLocations() {
		try {
			File csv = new File(Utils.SENSOR_FILE);
			if(!csv.exists()) {
				System.err.println("missing sensors file");
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
				if(row.length == 2) {
					sensorLocations.put(row[0], row[1]);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void populateColors() {
		colors.put("ignored prompt", new LegendItem("Ignored Prompt", Color.RED, LegendType.EVENT));
		colors.put("answered prompt", new LegendItem("Answered Prompt", Color.GREEN, LegendType.EVENT));
		colors.put("call", new LegendItem("Phone Call", Color.YELLOW, LegendType.EVENT));
		colors.put("text", new LegendItem("Phone SMS", Color.BLUE, LegendType.EVENT));
		colors.put("phone data", new LegendItem("Phone", Color.BLACK, LegendType.DATA));
		colors.put("watch data", new LegendItem("Watch", new Color(255,30,0,180), LegendType.DATA));
		colors.put("phone battery", new LegendItem("Phone", new Color(0,0,255,75), LegendType.BATTERY));
		colors.put("watch battery", new LegendItem("Watch", new Color(230,230,0,75), LegendType.BATTERY));
	}

	private static void parse() {
		File folder = new File(Utils.DATA_DIR);
		File[] listOfYears = folder.listFiles();

		if(listOfYears == null) {
			return;
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
				System.out.println(String.format("Finished a month %d of %d in %s", count++, monthCount, year.getName()));
			}
		}

		try {
			handleEventsAndAnnotations();
			Visualizer.prompts = getPrompts();
			Visualizer.sessions = getSessions();
		} catch (IOException|ParseException e) {
			e.printStackTrace();
		}
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
			System.err.println("missing survey dir");
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

			while (text != null && textR != null && (row = text.split(",")).length == 7) {
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
			if(row.length == 5) {
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
		if(!file.exists()) {
			System.err.println("missing sessions file");
			return ret;
		}

		Reader decoder = new InputStreamReader(new FileInputStream(file.getPath()), "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);
		buffered.readLine();

		String text;
		String[] row;

		while ((text = buffered.readLine()) != null && (row = text.split(",")).length == 13) {
			DateFormat df = new SimpleDateFormat("M/d/y k:m", Locale.ENGLISH);
			try {
				ret.add(new Event(df.parse(row[2]).getTime(), df.parse(row[3]).getTime(), row[1]));
			} catch(ParseException e) {
				// Do nothing
			}
		}
		buffered.close();
		return ret;
	}
}

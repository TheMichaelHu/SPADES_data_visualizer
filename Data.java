package dataVisualizer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

public class Data implements Comparable<Data> {
	String type;
	Timestamp time;
	double x_a;
	double y_a;
	double z_a;
	boolean isCorrupt;
	
	// Data represents a single row
	public Data(String type, String csvRow) {
		String[] row = csvRow.split(",");
		this.type = type;
		this.isCorrupt = parseTimeField(row[0]);
		this.x_a = Double.parseDouble(row[1]);
		this.y_a = Double.parseDouble(row[2]);
		this.z_a = Double.parseDouble(row[3]);
	}
	
	// Data is aggregation of many sequential rows
	public Data(String type, ArrayList<String> csvRows) throws Exception {
		this.type = type;
		this.x_a = 0;
		this.y_a = 0;
		this.z_a = 0;
		
		// use time of first row, if rows are ordered this should be fine
		int counter = 0;
		String[] row = csvRows.get(counter).split(",");
		while(!parseTimeField(row[0])) {
			row = csvRows.get(++counter).split(",");
			if(counter >= csvRows.size() - 1) {
				this.isCorrupt = true;
				break;
			}
		}
		
		double numRows = csvRows.size();
		
		// average the accelerations
		for(int i = 0; i < csvRows.size(); i++) {
			row = csvRows.get(i).split(",");
			try {
				this.x_a += Double.parseDouble(row[1])/numRows * 10000;
				this.y_a += Double.parseDouble(row[2])/numRows * 10000;
				this.z_a += Double.parseDouble(row[3])/numRows * 10000;
			} catch (Exception e) {
				System.out.println(csvRows.get(i));
				throw e;
			}
		}
	}
	
	private boolean parseTimeField(String timeField) {
		try {
			String date = timeField.split(" ")[0];
			String time = timeField.split(" ")[1];
	
			// year sometimes has strange leading characters
			double year = Double.parseDouble(date.split("-")[0]);
			double month = Double.parseDouble(date.split("-")[1]);
			double day = Double.parseDouble(date.split("-")[2]);
			
			double hour = Double.parseDouble(time.split(":")[0]);
			double min = Double.parseDouble(time.split(":")[1]);
			double sec = Double.parseDouble(time.split(":")[2]);
			

			this.time = new Timestamp(month, day, year, hour, min, sec);
			
		} catch(Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(Data that) {
		return this.time.compareTo(that.time);
	}
	
	public double getTime() {
		return time.getTimeInDays();
	}
	
	public double getXAccel() {
		return x_a;
	}
	
	public double getYAccel() {
		return y_a;
	}
	
	public double getZAccel() {
		return z_a;
	}
}

class Timestamp implements Comparable<Timestamp>{
	double month;
	double day;
	double year;
	double hour;
	double min;
	double sec;

	Timestamp(double month, double day, double year, double hour, double min, double sec) {
		this.month = month;
		this.day = day;
		this.year = year;
		this.hour = hour;
		this.min = min;
		this.sec = sec;
	}
	
	double getTimeInDays() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.clear();
		calendar.set((int)year, (int)month, (int)day, (int)hour, (int)min, (int)sec);
		double timeSecs = calendar.getTimeInMillis() / 1000L ;
		return timeSecs/60/60/24;
	}

	@Override
	public int compareTo(Timestamp that) {
		if(this.year != that.year) {
			return (int)(this.year - that.year);
		} else if(this.month != that.month) {
			return (int)(this.month - that.month);
		} else if(this.day != that.day) {
			return (int)(this.day - that.day);
		} else if(this.hour != that.hour) {
			return (int)(this.hour - that.hour);
		} else if(this.min != that.min) {
			return (int)(this.min - that.min);
		} else if(this.sec != that.sec) {
			return (int)(this.sec - that.sec);
		} else {
			return 0;
		}
	}
	
	public String toString() {
		return this.year + ":" + this.month + ":" + this.day + ":" + this.hour + ":" + this.min + ":" + this.sec;
	}
}

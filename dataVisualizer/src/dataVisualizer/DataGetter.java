package dataVisualizer;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

abstract class DataGetter {
    abstract ArrayList<Day> getData() throws IOException, ParseException;
    File month;
    File year;

    DataGetter(File month, File year) {
        this.month = month;
        this.year = year;
    }

    static boolean handleMissingData(Day day, File month, String HOUR, String DAY, int
            MONTH, int YEAR) {
        int count = 0;
        File[] listOfDays = month.listFiles();
        if(listOfDays != null) {
            for (File file : listOfDays) {
                if (file.getName().contains(DAY)) {
                    count++;
                    break;
                }
            }
        }
        if(count == 0)
            return false;

        Calendar cal = Calendar.getInstance();
        cal.set(YEAR, MONTH, Integer.parseInt(DAY), Integer.parseInt(HOUR), 0);
        day.data.add(new DataPoint(cal.getTimeInMillis(), null));

        cal.set(YEAR, MONTH, Integer.parseInt(DAY), Integer.parseInt(HOUR), 59);
        day.data.add(new DataPoint(cal.getTimeInMillis(), null));
        return true;
    }

    // MH: The variable names are so bad; I'm so sorry. There's no reason to use hours instead of time since epoch
    // but we do it anyways.
    void calculateData(InputStream gz, Day day) throws IOException, ParseException {
        Reader decoder = new InputStreamReader(gz, "UTF-8");
        BufferedReader buffered = new BufferedReader(decoder);
        try {
            buffered.readLine();
        } catch(EOFException e) {
            return;
        }
        String text = buffered.readLine();
        String[] row;
        ArrayList<DataPoint> recentData = new ArrayList<>();

        DateFormat df = new SimpleDateFormat("y-M-d k:m:s.S", Locale.ENGLISH);
        DateFormat tf = new SimpleDateFormat("k:m:s");
        Date startDate = df.parse(text.split(",")[0]);
        double startTime, lastTime;
        startTime = lastTime = getHourFromTime(tf.format(startDate));

        while (text != null && (row = text.split(",")).length == 4) {
            try {
                Date newDate = df.parse(row[0]);
                String newTime = tf.format(newDate);
                double newHour = getHourFromTime(newTime);
                final double startHour = lastTime;
                double sumMagnitude = 0.0;
                int count = 0;

                while (newHour - startHour < Utils.CHUNK && text != null && (row = text.split(",")).length == 4) {
                    count++;
                    Date datetime;

                    for (int i = 0; i < recentData.size(); i++) {
                        if (newHour - recentData.get(i).getTimeOfDay() < Utils.HIGH_PASS_CHUNK) {
                            break;
                        }
                        recentData.remove(i--);
                    }

                    try {
                        datetime = df.parse(row[0]);
                    } catch (ParseException e) {
                        text = buffered.readLine();
                        count--;
                        continue;
                    }

                    String time = tf.format(datetime);
                    newHour = getHourFromTime(time);

                    DataPoint data = new DataPoint(newDate.getTime(), Double.parseDouble(row[1]), Double.parseDouble(row[2]), Double.parseDouble(row[3]));
                    recentData.add(data);

                    double averageX = 0, averageY = 0, averageZ = 0;
                    for (DataPoint dp : recentData) {
                        averageX += dp.x / recentData.size();
                        averageY += dp.y / recentData.size();
                        averageZ += dp.z / recentData.size();
                    }

                    Double magnitude = Math.sqrt(Math.pow(data.x - averageX, 2) + Math.pow(data.y - averageY, 2) + Math.pow(data.z - averageZ, 2));

                    sumMagnitude += magnitude;
                    text = buffered.readLine();
                }

                lastTime += Utils.CHUNK;

                Date chunkDate = new Date(startDate.getTime() + (long) ((lastTime - startTime) * 1000 * 60 * 60));
                final Double finalAverage = count == 0 ? null : (Math.floor(sumMagnitude / count * 100) / 100);

                day.data.add(new DataPoint(chunkDate.getTime(), finalAverage));

                if (Utils.USE_DYNAMIC_HEIGHT) {
                    if (finalAverage > Utils.MAX_Y) {
                        Utils.MAX_Y = finalAverage;
                    } else if (finalAverage < Utils.MIN_Y) {
                        Utils.MIN_Y = finalAverage;
                    }
                }
            } catch (ParseException e) {
                text = buffered.readLine();
            }
        }

        while(24 - (lastTime - startTime) > Utils.CHUNK) {
            lastTime += Utils.CHUNK;
            Date chunkDate = new Date(startDate.getTime() + (long)((lastTime - startTime) * 1000 * 60 * 60));
            day.data.add(new DataPoint(chunkDate.getTime(), null));
        }

        buffered.close();
    }

    void calculateBatteryData(InputStream gz, Day day) throws IOException,
            ParseException {
        Reader decoder = new InputStreamReader(gz, "UTF-8");
        BufferedReader buffered = new BufferedReader(decoder);
        buffered.readLine();
        String text = buffered.readLine();
        String[] row;

        while (text != null && (row = text.split(",")).length == 5) {
            try {
                DateFormat df = new SimpleDateFormat("y-M-d k:m:s.S", Locale.ENGLISH);
                DateFormat tf = new SimpleDateFormat("k:m:s");

                Date aveDate = df.parse(row[0]);

                String aveTime = tf.format(aveDate);
                final double aveHour = getHourFromTime(aveTime);
                double hour = aveHour;

                double sumMagnitude = 0.0;
                int count = 0;

                do {
                    count++;
                    Date datetime;

                    try {
                        datetime = df.parse(row[0]);
                    } catch (ParseException e) {
                        text = buffered.readLine();
                        count--;
                        continue;
                    }

                    String time = tf.format(datetime);
                    hour = getHourFromTime(time);


                    Double magnitude = Double.parseDouble(row[3]);

                    sumMagnitude += magnitude;
                    text = buffered.readLine();

                } while (hour - aveHour < 0 && text != null && (row = text.split(",")).length == 5);
                final double finalSum = sumMagnitude;
                final int finalCount = count;
                day.data.add(new DataPoint(aveDate.getTime(), Math.floor(finalSum / finalCount * 100) / 100));
            } catch(ParseException e) {
                text = buffered.readLine();
            }
        }

        buffered.close();
    }

    private double getHourFromTime(String time) {
        return (double) Integer.parseInt(time.substring(0, time.indexOf(":")))
                + (double) Integer
                .parseInt(time.substring(time.indexOf(":") + 1, time.lastIndexOf(":")))
                / 60
                + (double) Integer.parseInt(time.substring(time.lastIndexOf(":") + 1)) / 3600;
    }
}

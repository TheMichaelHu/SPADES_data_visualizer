package dataVisualizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

class PhoneDataGetter extends DataGetter {

    PhoneDataGetter(File month, File year) {
        super(month, year);
    }

    ArrayList<Day> getData() throws IOException, ParseException  {
        ArrayList<Day> ret = new ArrayList<>();
        Day day = new Day();

        int MONTH = Integer.parseInt(month.getName()) - 1; // MH: Java.Calendar months a zero-indexed. Don't question it.
        int YEAR = Integer.parseInt(year.getName());
        YearMonth yearMonth = YearMonth.of(YEAR, MONTH + 1); // MH: But some Java 8 stuff is one-indexed. What a mess.
        for (int j = 1; j <= yearMonth.lengthOfMonth(); j++) {
            String DAY = ((Integer.toString(j).length() < 2) ? ("0" + Integer.toString(j)) : (Integer.toString(j)));
            Calendar cal = Calendar.getInstance();
            //noinspection MagicConstant
            cal.set(YEAR, MONTH, Integer.parseInt(DAY), 0, 0);
            day.date = cal.getTimeInMillis();

            for (int i = 0; i < 24; i++) {
                String HOUR = ((Integer.toString(i).length() < 2) ? ("0" + Integer.toString(i))
                        : (Integer.toString(i)));
                if(Utils.USE_DATE_RANGE) {
                    LocalDateTime start = LocalDateTime.of(YEAR, MONTH + 1, j, i, 1);
                    LocalDateTime end = LocalDateTime.of(YEAR, MONTH + 1, j, i, 59);
                    if(Utils.START_DATE.isAfter(end) || Utils.END_DATE.isBefore(start)) {
                        continue;
                    }
                }
                InputStream gz;

                try {
                    File temp = new File(month.getPath() + "/" + DAY + "/" + HOUR);
                    File[] listOfTemps = temp.listFiles();
                    File file = null;
                    if(listOfTemps == null) {
                        continue;
                    }

                    for(File alpha : listOfTemps) {
                        if(alpha.getName().toLowerCase().contains("phone") &&
                                alpha.getName().toLowerCase().contains("calibrated") &&
                                alpha.getName().toLowerCase().contains("sensor") &&
                                alpha.getName().toLowerCase().endsWith("csv.gz") && alpha.isFile()) {
                            file = alpha;
                            break;
                        }
                    }
                    if(file == null) {
                        if(!handleMissingData(day, month, HOUR, DAY, MONTH, YEAR)) {
                            break;
                        }
                        continue;
                    }
                    gz = new GZIPInputStream(new FileInputStream(file.getPath()));
                    this.calculateData(gz, day);
                } catch (Exception ignored) {
                    if(!handleMissingData(day, month, HOUR, DAY, MONTH, YEAR)) {
                        break;
                    }
                }
            }
            if(!day.data.isEmpty()) {
                ret.add(day);
            }
            day = new Day();
        }

        Collections.sort(ret);
        Visualizer.data.addAll(ret);
        return ret;
    }
}

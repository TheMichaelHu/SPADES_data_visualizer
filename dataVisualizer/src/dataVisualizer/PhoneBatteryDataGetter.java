package dataVisualizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

class PhoneBatteryDataGetter extends DataGetter {

    PhoneBatteryDataGetter(File month, File year) {
        super(month, year);
    }

    ArrayList<Day> getData() throws IOException, ParseException {
        ArrayList<Day> ret = new ArrayList<>();
        Day day = new Day();

        int MONTH = Integer.parseInt(month.getName()) - 1;
        int YEAR = Integer.parseInt(year.getName());
        YearMonth yearMonth = YearMonth.of(YEAR, MONTH + 1);
        for (int j = 1; j <= yearMonth.lengthOfMonth(); j++) {
            String DAY = ((Integer.toString(j).length() < 2) ? ("0" + Integer.toString(j)) : (Integer.toString(j)));
            Calendar cal = Calendar.getInstance();
            //noinspection MagicConstant
            cal.set(YEAR, MONTH, Integer.parseInt(DAY), 0, 0);
            day.date = cal.getTimeInMillis();

            for (int i = 0; i < 24; i++) {
                String HOUR = ((Integer.toString(i).length() < 2) ? ("0" + Integer.toString(i))
                        : (Integer.toString(i)));

                InputStream gz;

                try {
                    File temp = new File(month.getPath() + "/" + DAY + "/" + HOUR);
                    File[] listOfTemps = temp.listFiles();
                    File file = null;
                    if(listOfTemps == null) {
                        continue;
                    }

                    for(File alpha : listOfTemps) {
                        if(alpha.getName().toLowerCase().contains("battery") &&
                                alpha.getName().toLowerCase().contains("event") &&
                                alpha.getName().toLowerCase().endsWith("csv.gz") && alpha.isFile()) {
                            for(File beta : listOfTemps) {
                                if(beta.getName().toLowerCase().contains("phone") &&
                                        beta.getName().toLowerCase().contains(alpha.getName().toLowerCase()
                                                .substring(alpha.getName().toLowerCase().indexOf(".") + 1,
                                                        alpha.getName().toLowerCase().indexOf("-")))) {
                                    file = alpha;
                                    break;
                                }
                            }
                        }
                    }

                    if(file == null) {
                        if(!handleMissingData(day, month, HOUR, DAY, MONTH, YEAR)) {
                            break;
                        }
                        continue;
                    }
                    gz = new GZIPInputStream(new FileInputStream(file.getPath()));
                    this.calculateBatteryData(gz, day);
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
        Visualizer.battery.addAll(ret);
        return ret;
    }
}

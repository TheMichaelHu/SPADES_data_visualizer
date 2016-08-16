package dataVisualizer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

class DataPoint {
    long date;
    Double value;
    double x;
    double y;
    double z;

    DataPoint(long date, Double value) {
        this.date = date;
        this.value = value;
    }

    DataPoint(long date, double x, double y, double z) {
        this.date = date;
        this.x = x;
        this.y = y;
        this.z = z;
        this.value = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    double getTimeOfDay() {
        DateFormat tf = new SimpleDateFormat("k:m:s");
        String dataTime = tf.format(new Date(date));

        return (double) Integer.parseInt(dataTime.substring(0, dataTime.indexOf(":")))
                + (double) Integer
                .parseInt(dataTime.substring(dataTime.indexOf(":") + 1, dataTime.lastIndexOf(":")))
                / 60
                + (double) Integer.parseInt(dataTime.substring(dataTime.lastIndexOf(":") + 1)) / 3600;
    }

    public String toString() {
        return String.format("Date: %s, Value: %f", new Date(date), this.value);
    }
}

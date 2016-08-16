package dataVisualizer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

class Day implements Comparable<Day> {
    ArrayList<DataPoint> data;
    Long date;

    Day() {
        data = new ArrayList<>();
        date = null;
    }

    Calendar getCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(date));
        return cal;
    }

    public int compareTo(Day day) {
        return this.date.compareTo(day.date);
    }
}

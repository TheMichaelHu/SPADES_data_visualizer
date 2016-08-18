package dataVisualizer;

import java.util.ArrayList;

class Day implements Comparable<Day> {
    ArrayList<DataPoint> data;
    Long date;

    Day() {
        data = new ArrayList<>();
        date = null;
    }

    public int compareTo(Day day) {
        return this.date.compareTo(day.date);
    }
}

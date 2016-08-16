package dataVisualizer;

import java.util.Calendar;
import java.util.Date;

class Prompt {
    long date;
    boolean completed;
    String activity;
    String posture;

    Prompt(long date, boolean completed, String activity, String posture) {
        this.date = date;
        this.completed = completed;
        this.activity = activity;
        this.posture = posture;
    }

    Calendar getCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(date));
        return cal;
    }
}

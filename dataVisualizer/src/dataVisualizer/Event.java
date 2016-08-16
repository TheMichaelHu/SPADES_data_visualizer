package dataVisualizer;

import java.util.Calendar;
import java.util.Date;

class Event {
    long fromDate;
    Long toDate;
    String text;

    Event(long fromDate, Long toDate, String text) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.text = text;
    }

    Calendar getCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(fromDate));
        return cal;
    }

    int getLabelHeight() {
        int firstLetterVal = text.length() > 0 ? ((int)text.toLowerCase().charAt(0)-(int)'a') * 2 : 0;
        int secondLetterVal = text.length() > 1 ? text.toLowerCase().charAt(1) > 'm' ? 1 : 0 : 0;
        return 40 + firstLetterVal + secondLetterVal;
    }
}

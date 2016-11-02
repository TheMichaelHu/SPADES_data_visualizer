package dataVisualizer;

import java.awt.*;

class Event {
    long fromDate;
    Long toDate;
    String text;

    Event(long fromDate, Long toDate, String text) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.text = text;
        int maxLength = 15;
        if(text.length() > maxLength) {
            this.text = text.substring(0, maxLength) + "...";
        }
    }

    int getLabelHeight() {
        return this.text.hashCode() % (int)(Utils.HEIGHT * .8);
    }
    Color getLabelColor() {
        int colorVal = ((this.text.hashCode() % 255) + 255) % 255; // java % can be negative, it's absurd
        return new Color(colorVal, 0, colorVal);
    }
}

package dataVisualizer;

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
}

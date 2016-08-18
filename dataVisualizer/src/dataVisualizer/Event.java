package dataVisualizer;

class Event {
    long fromDate;
    Long toDate;
    String text;

    Event(long fromDate, Long toDate, String text) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.text = text;
    }

    int getLabelHeight() {
        int firstLetterVal =
                text.length() > 0 ? ((int)text.toLowerCase().charAt(0)-(int)'a') * 3 * Utils.HEIGHT/4/26 : 0;
        int secondLetterVal =
                text.length() > 1 ? ((int)text.toLowerCase().charAt(1)-(int)'a') * 3 * Utils.HEIGHT/4/26/26 : 0;

        return firstLetterVal + secondLetterVal;
    }
}

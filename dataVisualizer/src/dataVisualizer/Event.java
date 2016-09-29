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
        int firstLetterVal =
                text.length() > 0 ? ((int)text.toLowerCase().charAt(0)-(int)'a') * 4 * Utils.HEIGHT/5/26 : 0;
        int secondLetterVal =
                text.length() > 1 ? ((int)text.toLowerCase().charAt(1)-(int)'a') * 4 * Utils.HEIGHT/5/26/26 : 0;

        return this.text.hashCode() % (int)(Utils.HEIGHT * .8);
    }
}

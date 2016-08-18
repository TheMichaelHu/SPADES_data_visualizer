package dataVisualizer;

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
}

package dataVisualizer;

import java.io.IOException;
import java.text.ParseException;

class DataRunner implements Runnable {
    private DataGetter getter;

    DataRunner(DataGetter getter) {
        this.getter = getter;
    }

    public void run(){
        try {
            getter.getData();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}

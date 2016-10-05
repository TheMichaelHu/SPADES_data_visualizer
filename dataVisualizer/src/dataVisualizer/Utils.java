package dataVisualizer;

import java.io.File;
import java.time.LocalDateTime;
import java.time.Month;

class Utils {
    static int WIDTH = 3000;
    static int HEIGHT = 500;
    static double MIN_Y = 0.0;
    static double MAX_Y = 0.0;
    static double MAX_Y_CAP = 2.0;
    static double CHUNK = 60 / 3600.0;
    static final double HIGH_PASS_CHUNK = 5 / 3600.0;
    static final String[] IGNORED_ANNOTATIONS = new String[]{"unlabelled", "city", "1 mph", "2 mph"};

    static String[] SENSORS = new String[]{"phone", "watch", "actigraph"};
    static String HOME_DIR = "./";
    static String DATA_DIR = HOME_DIR + "MasterSynced";
    static String ROOT_DIR = HOME_DIR + "../../";
    static String SURVEY_DIR = ROOT_DIR + "survey";
    static String SESSION_FILE = ROOT_DIR + "Sessions.csv";
    static String SENSOR_FILE = ROOT_DIR + "sensor_locations.csv";
    static String TITLE = "[Insert default title]";
    static String TARGET_DIR = "./";

    static final String IMAGE_EXT = "png";


    static LocalDateTime START_DATE = LocalDateTime.of(2016, Month.FEBRUARY, 16, 9, 1);
    static LocalDateTime END_DATE = LocalDateTime.of(2016, Month.FEBRUARY, 16, 11, 59);
    static boolean USE_DATE_RANGE = false;
    static boolean LAB_ONLY = false;
    static final boolean USE_DYNAMIC_HEIGHT = true;

    static ChartType CHART_TYPE = ChartType.byWeek;

    enum ChartType {
        byWeek, byDay
    }

    static void updateDirs() {
        if(HOME_DIR != null) {
            DATA_DIR = HOME_DIR + "MasterSynced";
            if(!new File(DATA_DIR).isDirectory()) {
                throw new RuntimeException("Can't find MasterSynced");
            }
            ROOT_DIR = HOME_DIR + "../../";
            SURVEY_DIR = ROOT_DIR + "survey";
            SESSION_FILE = ROOT_DIR + "Sessions.csv";
            SENSOR_FILE = ROOT_DIR + "sensor_locations.csv";
            TITLE = HOME_DIR.split("/")[HOME_DIR.split("/").length-1];
            if(LAB_ONLY) {
                TITLE += "_lab";
            }
        }
    }
}

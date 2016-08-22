package dataVisualizer;

import java.io.File;

class Utils {
    static final int WIDTH = 3000;
    static final int HEIGHT = 500;
    static double MIN_Y = 0.0;
    static double MAX_Y = 0.0;
    static double MAX_Y_CAP = 2.0;
    static final double CHUNK = 60 / 3600.0;
    static final double HIGH_PASS_CHUNK = 5 / 3600.0;
    static final String[] IGNORED_ANNOTATIONS = new String[]{"unlabelled", "city", "1 mph", "2 mph"};

    static String HOME_DIR = "[Insert default dir]";
    static String DATA_DIR = HOME_DIR + "MasterSynced";
    static String SURVEY_DIR = HOME_DIR + "../../survey";
    static String SESSION_FILE = HOME_DIR + "../../Sessions.csv";
    static String SENSOR_FILE = HOME_DIR + "../../sensor_location.csv";

    static String TITLE = "[Insert default title]";

    static String TARGET_DIR = "[Insert default file]";

    static final String IMAGE_EXT = "png";

    static final boolean USE_DYNAMIC_HEIGHT = true;

    static final ChartType CHART_TYPE = ChartType.byWeek;

    enum ChartType {
        byWeek, byMonth
    }

    static void updateDirs() {
        if(HOME_DIR != null) {
            DATA_DIR = HOME_DIR + "MasterSynced";
            if(!new File(DATA_DIR).isDirectory()) {
                throw new RuntimeException("Can't find MasterSynced");
            }
            SURVEY_DIR = HOME_DIR + "../../survey";
            SESSION_FILE = HOME_DIR + "../../Sessions.csv";
            SENSOR_FILE = HOME_DIR + "../../sensor_location.csv";
            TITLE = HOME_DIR.split("/")[HOME_DIR.split("/").length-1];
        }
    }
}

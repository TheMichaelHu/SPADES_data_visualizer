package dataVisualizer;

import java.awt.*;

class LegendItem {
    private String name;
    Color color;
    LegendType type;

    LegendItem(String name, Color color, LegendType type) {
        this.name = name;
        this.color = color;
        this.type = type;
    }
}

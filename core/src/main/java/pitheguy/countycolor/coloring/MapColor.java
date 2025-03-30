package pitheguy.countycolor.coloring;

import com.badlogic.gdx.graphics.Color;

public enum MapColor {
    GREEN(Color.GREEN),
    CYAN(Color.CYAN),
    YELLOW(new Color(1, 0.9f, 0, 1)),
    MAGENTA(Color.MAGENTA);

    private final Color color;

    MapColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public String getSerializedName() {
        return name().toLowerCase();
    }

    public static MapColor fromSerializedName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

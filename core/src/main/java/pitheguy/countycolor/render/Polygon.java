package pitheguy.countycolor.render;

import com.badlogic.gdx.math.Vector2;

import java.util.Collection;
import java.util.List;

public class Polygon {
    private final List<Vector2> points;
    private float minX;
    private float minY;
    private float maxX;
    private float maxY;

    public Polygon(List<Vector2> points) {
        this.points = points;
        recalculateBounds();
    }

    public List<Vector2> getPoints() {
        return points;
    }

    public void recalculateBounds() {
        minX = (float) points.stream().mapToDouble(v -> v.x).min().getAsDouble();
        minY = (float) points.stream().mapToDouble(v -> v.y).min().getAsDouble();
        maxX = (float) points.stream().mapToDouble(v -> v.x).max().getAsDouble();
        maxY = (float) points.stream().mapToDouble(v -> v.y).max().getAsDouble();
    }

    public boolean isAdjacentTo(Polygon other) {
        if (!boundingBoxOverlaps(other)) return false;
        for (int i = 0; i < points.size() - 1; i++) {
            Vector2 point = points.get(i);
            int otherIndex = other.points.indexOf(point);
            if (otherIndex < 0) continue;
            if (points.get(i + 1).equals(other.points.get(otherIndex + 1))) return true;
            if (otherIndex > 0 && points.get(i + 1).equals(other.points.get(otherIndex - 1))) return true;
        }
        return false;
    }

    private boolean boundingBoxOverlaps(Polygon other) {
        return minX <= other.maxX &&
               maxX >= other.minX &&
               minY <= other.maxY &&
               maxY >= other.minY;
    }
}

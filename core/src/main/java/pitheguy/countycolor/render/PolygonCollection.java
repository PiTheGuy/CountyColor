package pitheguy.countycolor.render;

import com.badlogic.gdx.math.Vector2;
import pitheguy.countycolor.render.util.RenderConst;

import java.util.Collection;
import java.util.List;

public class PolygonCollection {
    private final List<List<Vector2>> polygons;
    private float minX, minY, maxX, maxY;

    public PolygonCollection(List<List<Vector2>> polygons) {
        this.polygons = polygons;
        recalculateBounds();
    }

    public List<List<Vector2>> getPolygons() {
        return polygons;
    }

    public float getMinX() {
        return minX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMaxY() {
        return maxY;
    }

    public boolean boundsCheck(Vector2 point) {
        Vector2 scaled = point.cpy().scl(2f / RenderConst.RENDER_SIZE);
        return scaled.x > minX && scaled.x < maxX && scaled.y > minY && scaled.y < maxY;
    }

    public void recalculateBounds() {
        minX = (float) polygons.stream().flatMap(Collection::stream).mapToDouble(v -> v.x).min().getAsDouble();
        minY = (float) polygons.stream().flatMap(Collection::stream).mapToDouble(v -> v.y).min().getAsDouble();
        maxX = (float) polygons.stream().flatMap(Collection::stream).mapToDouble(v -> v.x).max().getAsDouble();
        maxY = (float) polygons.stream().flatMap(Collection::stream).mapToDouble(v -> v.y).max().getAsDouble();
    }

    public boolean boundingBoxOverlaps(PolygonCollection other) {
        return minX <= other.maxX &&
               maxX >= other.minX &&
               minY <= other.maxY &&
               maxY >= other.minY;
    }

    public boolean isAdjacentTo(PolygonCollection other) {
        if (!boundingBoxOverlaps(other)) return false;
        for (List<Vector2> points : polygons) {
            Polygon polygon = new Polygon(points);
            for (List<Vector2> otherPoints : other.polygons) {
                Polygon otherPolygon = new Polygon(otherPoints);
                if (polygon.isAdjacentTo(otherPolygon)) return true;
            }
        }
        return false;
    }

}

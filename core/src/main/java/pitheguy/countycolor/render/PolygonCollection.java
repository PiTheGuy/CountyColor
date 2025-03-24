package pitheguy.countycolor.render;

import com.badlogic.gdx.math.Vector2;
import pitheguy.countycolor.render.util.RenderConst;

import java.util.*;

public class PolygonCollection {
    private final List<List<Vector2>> polygons;
    private final float minX, minY, maxX, maxY;

    public PolygonCollection(List<List<Vector2>> polygons) {
        this.polygons = polygons;
        minX = (float) polygons.stream().flatMap(Collection::stream).mapToDouble(v -> v.x).min().getAsDouble();
        minY = (float) polygons.stream().flatMap(Collection::stream).mapToDouble(v -> v.y).min().getAsDouble();
        maxX = (float) polygons.stream().flatMap(Collection::stream).mapToDouble(v -> v.x).max().getAsDouble();
        maxY = (float) polygons.stream().flatMap(Collection::stream).mapToDouble(v -> v.y).max().getAsDouble();
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
}

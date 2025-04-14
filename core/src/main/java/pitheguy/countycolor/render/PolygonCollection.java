package pitheguy.countycolor.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;

import java.util.*;

import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

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
        Vector2 scaled = point.cpy().scl(2f / RENDER_SIZE);
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

    public boolean isVisibleToCamera(OrthographicCamera camera) {
        float camMinX = camera.position.x - (camera.viewportWidth * camera.zoom) / 2f;
        float camMaxX = camera.position.x + (camera.viewportWidth * camera.zoom) / 2f;
        float camMinY = camera.position.y - (camera.viewportHeight * camera.zoom) / 2f;
        float camMaxY = camera.position.y + (camera.viewportHeight * camera.zoom) / 2f;

        return maxX * RENDER_SIZE / 2 >= camMinX && minX * RENDER_SIZE / 2 <= camMaxX &&
               maxY * RENDER_SIZE / 2 >= camMinY && minY * RENDER_SIZE / 2 <= camMaxY;
    }

    public List<List<Vector2>> getSharedEdges(PolygonCollection other) {
        List<List<Vector2>> sharedEdges = new ArrayList<>();
        if (!boundingBoxOverlaps(other)) return sharedEdges;
        for (List<Vector2> points : polygons) {
            Polygon polygon = new Polygon(points);
            for (List<Vector2> otherPoints : other.polygons) {
                Polygon otherPolygon = new Polygon(otherPoints);
                sharedEdges.addAll(polygon.getSharedEdges(otherPolygon));
            }
        }
        return sharedEdges;
    }

    public PolygonCollection copy() {
        List<List<Vector2>> newPolygons = new ArrayList<>();
        for (List<Vector2> polygon : polygons) {
            List<Vector2> newPolygon = new ArrayList<>(polygon);
            newPolygon.replaceAll(Vector2::cpy);
            newPolygons.add(newPolygon);
        }
        return new PolygonCollection(newPolygons);
    }

}

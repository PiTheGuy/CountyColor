package pitheguy.countycolor.render.renderer;

import clipper2.core.*;
import clipper2.offset.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.metadata.CountyData;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.util.RenderUtil;

import java.util.*;

import static pitheguy.countycolor.render.util.RenderConst.*;

public class CountyRenderer extends CountyLevelRenderer {
    private final CountyData.County county;
    private float highlightTime = 0;
    private PolygonCollection polygons;
    private int totalGridSquares = -1;

    public CountyRenderer(CountyData.County county) {
        this.county = county;
    }

    private CountyData.County getCounty() {
        ensureLoadingFinished();
        return counties.get(counties.keySet().iterator().next());
    }

    public void renderCounty(OrthographicCamera camera) {
        ensureLoadingFinished();
        updateCamera(camera);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLACK);
        if (!polygons.isVisibleToCamera(camera)) return;
        for (List<Vector2> points : polygons.getPolygons())
            RenderUtil.drawThickPolylineCulled(camera, shapeRenderer, points, OUTLINE_THICKNESS, true);
        shapeRenderer.end();
    }

    public void renderCounty(OrthographicCamera camera, float scale) {
        ensureLoadingFinished();
        updateCamera(camera);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLACK);
        if (!polygons.isVisibleToCamera(camera)) return;
        for (List<Vector2> points : polygons.getPolygons()) {
            List<Vector2> pointsCopy = new ArrayList<>(points);
            pointsCopy.replaceAll(vector2 -> vector2.cpy().scl(scale));
            RenderUtil.drawThickPolylineCulled(camera, shapeRenderer, pointsCopy, OUTLINE_THICKNESS, true);
        }
        shapeRenderer.end();
    }

    public void renderCountyFilled(OrthographicCamera camera, float scale, MapColor color) {
        ensureLoadingFinished();
        updateCamera(camera);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (List<Vector2> points : polygons.getPolygons()) {
            shapeRenderer.setColor(color.getColor());
            RenderUtil.renderFilledPolygon(shapeRenderer, points, triangles.computeIfAbsent(points, RenderUtil::triangulate), scale);
            shapeRenderer.setColor(Color.BLACK);
            List<Vector2> pointsCopy = new ArrayList<>(points);
            pointsCopy.replaceAll(vector2 -> vector2.cpy().scl(scale));
            RenderUtil.drawThickPolyline(shapeRenderer, pointsCopy, (float) OUTLINE_THICKNESS);
        }
        shapeRenderer.end();
    }

    public void renderHighlight(OrthographicCamera camera, float delta) {
        if (highlightTime > 0) {
            updateCamera(camera);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            float colorDelta = getDelta();
            fillSubregion(getCounty().getPolygons(), new Color(1, 1 - colorDelta, 1 - colorDelta, 1));
            shapeRenderer.end();
            highlightTime -= delta;
        }
    }

    @Override
    protected void loadShapes() {
        counties = StateRenderer.rel(Map.of(county.getName(), county));
        JsonReader reader = new JsonReader();
        JsonValue countyJson = reader.parse(Gdx.files.internal("metadata/counties/" + county.getGeoId() + ".json"));
        polygons = relativize(Map.of(county.getName(), loadSubregion(countyJson))).entrySet().iterator().next().getValue();
        totalGridSquares = computeTotalGridSquares();
    }

    public void highlightUncoloredAreas() {
        highlightTime = 1.5f;
    }

    private float getDelta() {
        if (highlightTime > 1) return 1 - ((highlightTime - 1) * 2);
        if (highlightTime > 0.5f) return 1;
        return highlightTime * 2;
    }

    public boolean isCoordinateWithinCounty(Vector2 coordinate) {
        return polygons.contains(coordinate.cpy().scl(2f / RENDER_SIZE));
    }

    public int getTotalGridSquares() {
        ensureLoadingFinished();
        return totalGridSquares;
    }

    private int computeTotalGridSquares() {
        float totalPerimeter = 0;
        float totalArea = 0;
        for (List<Vector2> shape : polygons.getPolygons()) {
            totalPerimeter += RenderUtil.calculatePerimeter(shape);
            totalArea += RenderUtil.calculateArea(shape);
        }
        float ratio = totalPerimeter / totalArea;
        float multiplier = 0.00173616f * ratio + 0.999478f;

        int halfGridSize = COLORING_SIZE / 2;
        int total = 0;
        List<List<Vector2>> scaledPolygons = new ArrayList<>();
        polygons.getPolygons().parallelStream().forEach(poly -> {
            List<Vector2> scaled = new ArrayList<>();
            for (Vector2 p : poly) scaled.add(p.cpy().scl(RENDER_SIZE / 2f));
            List<List<Vector2>> shrunk = shrinkPolygon(scaled);
            scaledPolygons.addAll(shrunk);
        });
        for (int gridY = 0; gridY < COLORING_SIZE; gridY++) {
            float worldY = ((float) gridY + 0.5f - halfGridSize) / COLORING_RESOLUTION;
            List<Interval> intervals = new ArrayList<>();
            for (List<Vector2> poly : scaledPolygons) {
                List<Float> xIntersections = new ArrayList<>();
                int n = poly.size();
                for (int i = 0, j = n - 1; i < n; j = i++) {
                    float y1 = poly.get(j).y;
                    float y2 = poly.get(i).y;
                    if ((y1 <= worldY && y2 > worldY) || (y2 <= worldY && y1 > worldY)) {
                        float x1 = poly.get(j).x;
                        float x2 = poly.get(i).x;
                        float intersectX = x1 + (worldY - y1) * (x2 - x1) / (y2 - y1);
                        xIntersections.add(intersectX);
                    }
                }
                Collections.sort(xIntersections);
                for (int k = 0; k < xIntersections.size() - 1; k += 2) {
                    float startX = xIntersections.get(k);
                    float endX = xIntersections.get(k + 1);
                    intervals.add(new Interval(startX, endX));
                }
            }
            intervals = mergeIntervals(intervals);
            for (Interval inter : intervals) {
                int startGridX = (int) Math.ceil(inter.start * COLORING_RESOLUTION + halfGridSize);
                int endGridX = (int) Math.floor(inter.end * COLORING_RESOLUTION + halfGridSize);
                if (endGridX >= startGridX) {
                    total += (endGridX - startGridX + 1);
                }
            }
        }
        return (int) (total * multiplier);
    }

    private List<List<Vector2>> shrinkPolygon(List<Vector2> polygon) {
        Path64 path = new Path64();
        float scale = 1e6f;
        float amount = OUTLINE_THICKNESS / 2f;
        for (Vector2 p : polygon) path.add(new Point64(p.x * scale, p.y * scale));
        ClipperOffset offset = new ClipperOffset();
        offset.AddPath(path, JoinType.Square, EndType.Polygon);
        Paths64 solution = new Paths64();
        offset.Execute(-amount * scale, solution);
        List<List<Vector2>> result = new ArrayList<>();
        for (Path64 p : solution) {
            List<Vector2> shape = new ArrayList<>();
            for (Point64 point : p)
                shape.add(new Vector2(point.x / scale, point.y / scale));
            result.add(shape);
        }
        return result;
    }

    private List<Interval> mergeIntervals(List<Interval> intervals) {
        List<Interval> merged = new ArrayList<>();
        if (intervals.isEmpty()) return merged;

        intervals.sort((a, b) -> Float.compare(a.start, b.start));
        Interval current = intervals.get(0);
        for (int i = 1; i < intervals.size(); i++) {
            Interval next = intervals.get(i);
            if (next.start <= current.end) {
                current.end = Math.max(current.end, next.end);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private static class Interval {
        float start;
        float end;

        Interval(float start, float end) {
            this.start = start;
            this.end = end;
        }
    }
}

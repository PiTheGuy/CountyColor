package pitheguy.countycolor.render.renderer;

import clipper2.core.*;
import clipper2.offset.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.render.util.RenderUtil;
import pitheguy.countycolor.util.DebugFlags;

import java.util.*;
import java.util.Collections;
import java.util.concurrent.*;

import static pitheguy.countycolor.render.util.RenderConst.*;

public class CountyRenderer {
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final Future<List<List<Vector2>>> shapesFuture;
    private final List<ShortArray> triangles = new ArrayList<>();
    private List<List<Vector2>> shapes;

    public CountyRenderer(String county, String state) {
        this.shapesFuture = loadCounty(county, StateRenderer.getIdForState(state));
    }

    public void renderCounty(Camera camera) {
        ensureLoadingFinished();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLACK);
        for (List<Vector2> points : shapes) {
            List<Vector2> pointsCopy = new ArrayList<>(points);
            pointsCopy.replaceAll(Vector2::cpy);
            RenderUtil.drawThickPolyline(shapeRenderer, pointsCopy, (float) OUTLINE_THICKNESS, RENDER_SIZE);
        }
        shapeRenderer.end();
    }

    public void renderCountyFilled(Camera camera, float scale, MapColor color) {
        ensureLoadingFinished();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < shapes.size(); i++) {
            shapeRenderer.setColor(color.getColor());
            List<Vector2> points = shapes.get(i);
            RenderUtil.renderFilledPolygon(shapeRenderer, points, triangles.get(i), scale);
            shapeRenderer.setColor(Color.BLACK);
            List<Vector2> pointsCopy = new ArrayList<>(points);
            pointsCopy.replaceAll(vector2 -> vector2.cpy().scl(scale));
            RenderUtil.drawThickPolyline(shapeRenderer, pointsCopy, (float) OUTLINE_THICKNESS, RENDER_SIZE);
        }
        shapeRenderer.end();
    }

    private void ensureLoadingFinished() {
        if (shapes == null) {
            try {
                shapes = shapesFuture.get();
                for (List<Vector2> points : shapes) triangles.add(RenderUtil.triangulate(points));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Future<List<List<Vector2>>> loadCounty(String county, String stateId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal("counties.json"));
            JsonValue array = root.get("features");
            for (JsonValue country : array) {
                JsonValue properties = country.get("properties");
                if (!properties.getString("STATEFP").equals(stateId)) continue;
                if (!properties.getString("NAME").equals(county)) continue;
                JsonValue geometry = country.get("geometry");
                String type = geometry.getString("type");
                JsonValue coordinates = geometry.get("coordinates");
                List<JsonValue> shapesJson = switch (type) {
                    case "Polygon" -> List.of(coordinates);
                    case "MultiPolygon" -> {
                        List<JsonValue> polys = new ArrayList<>();
                        for (JsonValue polygon : coordinates) {
                            polys.add(polygon);
                        }
                        yield polys;
                    }
                    default -> throw new IllegalStateException("Unexpected type: " + type);
                };
                List<List<Vector2>> shapes = new ArrayList<>();
                for (JsonValue arr : shapesJson) {
                    JsonValue outer = arr.get(0); // use only the outer ring
                    List<Vector2> points = new ArrayList<>();
                    for (JsonValue point : outer) {
                        float lat = point.getFloat(0);
                        float lng = point.getFloat(1);
                        points.add(new Vector2(lat, lng));
                    }
                    shapes.add(points);
                }
                float minX = (float) shapes.stream().flatMap(Collection::stream).mapToDouble(v -> v.x).min().getAsDouble();
                float minY = (float) shapes.stream().flatMap(Collection::stream).mapToDouble(v -> v.y).min().getAsDouble();
                float maxX = (float) shapes.stream().flatMap(Collection::stream).mapToDouble(v -> v.x).max().getAsDouble();
                float maxY = (float) shapes.stream().flatMap(Collection::stream).mapToDouble(v -> v.y).max().getAsDouble();

                if (maxX - minX > 180) {
                    for (List<Vector2> shape : shapes) shape.replaceAll(CountyRenderer::normalize);
                    minX = (float) shapes.stream().flatMap(Collection::stream).mapToDouble(v -> v.x).min().getAsDouble();
                    maxX = (float) shapes.stream().flatMap(Collection::stream).mapToDouble(v -> v.x).max().getAsDouble();
                }

                float xRange = maxX - minX;
                float yRange = maxY - minY;
                float maxRange = Math.max(xRange, yRange);
                if (xRange < yRange) {
                    float mid = minX + xRange / 2;
                    minX = mid - maxRange / 2;
                    maxX = mid + maxRange / 2;
                } else {
                    float mid = minY + yRange / 2;
                    minY = mid - maxRange / 2;
                    maxY = mid + maxRange / 2;
                }

                List<List<Vector2>> relativeShapes = new ArrayList<>();
                for (List<Vector2> points : shapes) {
                    List<Vector2> relativePoints = new ArrayList<>();
                    for (Vector2 point : points) relativePoints.add(relativize(point, minX, maxX, minY, maxY));
                    relativeShapes.add(relativePoints);
                }
                return relativeShapes;
            }
            return null;
        });
    }

    private static Vector2 relativize(Vector2 point, float minX, float maxX, float minY, float maxY) {
        float xDiff = maxX - minX;
        float yDiff = maxY - minY;
        float x = ((point.x - minX) / xDiff) * 2f - 1f;
        float y = ((point.y - minY) / yDiff) * 2f - 1f;
        return new Vector2(x, y);
    }

    private static Vector2 normalize(Vector2 point) {
        if (point.x < 0) return new Vector2(point.x + 360, point.y);
        return point;
    }

    public boolean isCoordinateWithinCounty(Vector2 coordinate, int renderSize) {
        List<List<Vector2>> shapes = new ArrayList<>(this.shapes);
        for (int i = 0, shapesSize = shapes.size(); i < shapesSize; i++) {
            List<Vector2> shapeCopy = new ArrayList<>(shapes.get(i));
            shapeCopy.replaceAll(point -> point.cpy().scl(renderSize / 2f));
            shapes.set(i, shapeCopy);
        }
        return shapes.stream().anyMatch(polygon -> RenderUtil.pointInPolygon(coordinate, polygon));
    }

    public int computeTotalGridSquares() {
        ensureLoadingFinished();
        int coloringSize = RENDER_SIZE * COLORING_RESOLUTION;
        int halfGridSize = coloringSize / 2;
        int total = 0;
        List<List<Vector2>> scaledPolygons = new ArrayList<>();
        for (List<Vector2> poly : shapes) {
            List<Vector2> scaled = new ArrayList<>();
            for (Vector2 p : poly) scaled.add(new Vector2(p).scl(RENDER_SIZE / 2f));
            List<List<Vector2>> shrunk = shrinkPolygon(scaled);
            scaledPolygons.addAll(shrunk);
        }
        for (int gridY = 0; gridY < coloringSize; gridY++) {
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
        return total;
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

    public void dispose() {
        shapeRenderer.dispose();
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

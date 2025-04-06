package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.Zoom;
import pitheguy.countycolor.render.util.RenderUtil;
import pitheguy.countycolor.util.Util;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

import static pitheguy.countycolor.render.util.RenderConst.OUTLINE_THICKNESS;
import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public abstract class RegionRenderer implements Disposable {
    private static final ExecutorService SHAPE_LOAD_EXECUTOR = Executors.newCachedThreadPool();

    private final Future<Map<String, PolygonCollection>> shapesFuture;
    protected Map<String, PolygonCollection> shapes;
    protected final ShapeRenderer shapeRenderer = new ShapeRenderer();
    protected final Map<List<Vector2>, ShortArray> triangles = new HashMap<>();


    public RegionRenderer(String sourceFilePath, Predicate<JsonValue> predicate) {
        this(sourceFilePath, predicate, null);
    }

    public RegionRenderer(String sourceFilePath, Predicate<JsonValue> predicate, String duplicatePreventionKey) {
        shapesFuture = loadShapesAsync(sourceFilePath, predicate, duplicatePreventionKey);
    }

    protected void renderRegion(OrthographicCamera camera, boolean thick, boolean scaleThickness) {
        ensureLoadingFinished();
        shapeRenderer.begin(thick ? ShapeRenderer.ShapeType.Filled : ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);
        for (String subregion : shapes.keySet()) {
            PolygonCollection subregionPolygons = shapes.get(subregion);
            for (List<Vector2> points : subregionPolygons.getPolygons()) {
                if (thick)
                    RenderUtil.drawThickPolyline(shapeRenderer, points, scaleThickness ? OUTLINE_THICKNESS * camera.zoom : OUTLINE_THICKNESS);
                else for (int i = 0; i < points.size() - 1; i++) {
                    Vector2 point = points.get(i);
                    Vector2 endPoint = points.get(i + 1);
                    shapeRenderer.line(point.x * RENDER_SIZE / 2f, point.y * RENDER_SIZE / 2f, endPoint.x * RENDER_SIZE / 2f, endPoint.y * RENDER_SIZE / 2f);
                }
            }
        }
        shapeRenderer.end();
    }

    protected void renderBackground() {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    protected void fillSubregion(String subregion, Color color) {
        ensureLoadingFinished();
        shapeRenderer.setColor(color);
        PolygonCollection state = shapes.get(subregion);
        for (List<Vector2> points : state.getPolygons()) {
            RenderUtil.renderFilledPolygon(shapeRenderer, points, triangles.computeIfAbsent(points, RenderUtil::triangulate), 1);
        }
    }

    protected void updateCamera(OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
    }

    private Future<Map<String, PolygonCollection>> loadShapesAsync(String sourceFilePath, Predicate<JsonValue> predicate, String duplicatePreventionKey) {
        return SHAPE_LOAD_EXECUTOR.submit(() -> loadShapes(sourceFilePath, predicate, duplicatePreventionKey));
    }

    protected Map<String, PolygonCollection> loadShapes(String sourceFilePath, Predicate<JsonValue> predicate, String duplicatePreventionKey) {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(Gdx.files.internal(sourceFilePath));
        JsonValue array = root.get("features");
        Map<String, PolygonCollection> shapes = new HashMap<>();
        for (JsonValue subregion : array) {
            JsonValue properties = subregion.get("properties");
            if (!predicate.test(properties)) continue;
            String subregionName = properties.getString("NAME");
            String duplicatePrevention = duplicatePreventionKey == null ? "" : properties.getString(duplicatePreventionKey);
            PolygonCollection polygons = loadSubregion(subregion);
            shapes.put(subregionName + duplicatePrevention, polygons);
        }
        postProcessShapes(shapes);
        return relativize(shapes);
    }

    protected void postProcessShapes(Map<String, PolygonCollection> shapes) {
    }

    protected static PolygonCollection loadSubregion(JsonValue subregion) {
        JsonValue geometry = subregion.get("geometry");
        String type = geometry.getString("type");
        JsonValue coordinates = geometry.get("coordinates");
        List<JsonValue> shapesJson;
        switch (type) {
            case "Polygon":
                shapesJson = new ArrayList<>();
                shapesJson.add(coordinates);
                break;
            case "MultiPolygon":
                List<JsonValue> polys = new ArrayList<>();
                for (JsonValue polygon : coordinates) polys.add(polygon);
                shapesJson = polys;
                break;
            default:
                throw new IllegalStateException("Unexpected type: " + type);
        }
        List<List<Vector2>> polygons = new ArrayList<>();
        for (JsonValue arr : shapesJson) {
            JsonValue outer = arr.get(0); // only use outer ring
            List<Vector2> points = new ArrayList<>();
            for (JsonValue point : outer) {
                float lat = point.getFloat(0);
                float lng = point.getFloat(1);
                points.add(new Vector2(lat, lng));
            }
            polygons.add(points);
        }
        return new PolygonCollection(polygons);
    }

    protected void ensureLoadingFinished() {
        if (shapes == null) shapes = Util.getFutureValue(shapesFuture);
    }

    public boolean isDoneLoading() {
        return shapesFuture.isDone();
    }

    protected static Map<String, PolygonCollection> relativize(Map<String, PolygonCollection> shapes) {
        float minX = (float) shapes.values().stream().mapToDouble(PolygonCollection::getMinX).min().getAsDouble();
        float minY = (float) shapes.values().stream().mapToDouble(PolygonCollection::getMinY).min().getAsDouble();
        float maxX = (float) shapes.values().stream().mapToDouble(PolygonCollection::getMaxX).max().getAsDouble();
        float maxY = (float) shapes.values().stream().mapToDouble(PolygonCollection::getMaxY).max().getAsDouble();
        Map<String, PolygonCollection> relativeShapes = new HashMap<>();
        for (Map.Entry<String, PolygonCollection> entry : shapes.entrySet()) {
            List<List<Vector2>> county = new ArrayList<>();
            for (List<Vector2> points : entry.getValue().getPolygons()) {
                List<Vector2> relativePoints = new ArrayList<>();
                for (Vector2 point : points) relativePoints.add(relativizePoint(point, minX, maxX, minY, maxY));
                county.add(relativePoints);
            }
            relativeShapes.put(entry.getKey(), new PolygonCollection(county));
        }
        return relativeShapes;
    }

    protected static Vector2 relativizePoint(Vector2 point, float minX, float maxX, float minY, float maxY) {
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
        float xDiff = maxX - minX;
        float yDiff = maxY - minY;
        float x = ((point.x - minX) / xDiff) * 2f - 1f;
        float y = ((point.y - minY) / yDiff) * 2f - 1f;
        return new Vector2(x, y);
    }

    public String getSubregionAtCoords(Vector2 coordinate) {
        for (Map.Entry<String, PolygonCollection> entry : shapes.entrySet()) {
            PolygonCollection subregion = entry.getValue();
            if (!subregion.boundsCheck(coordinate)) continue;
            for (List<Vector2> polygon : subregion.getPolygons())
                if (RenderUtil.pointInPolygon(coordinate.cpy().scl(2f / RENDER_SIZE), polygon))
                    return entry.getKey();
        }
        return null;
    }

    public Zoom getTargetZoom(String subregion) {
        PolygonCollection subregionPolygons = shapes.get(subregion);
        float minX = subregionPolygons.getMinX();
        float minY = subregionPolygons.getMinY();
        float maxX = subregionPolygons.getMaxX();
        float maxY = subregionPolygons.getMaxY();
        float xRange = (maxX - minX) / 2;
        float yRange = (maxY - minY) / 2;
        float zoom = Math.max(xRange, yRange) * RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float xCenter = (minX + maxX) / 4f * RENDER_SIZE;
        float yCenter = (minY + maxY) / 4f * RENDER_SIZE;
        return new Zoom(new Vector2(xCenter, yCenter), zoom);
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}

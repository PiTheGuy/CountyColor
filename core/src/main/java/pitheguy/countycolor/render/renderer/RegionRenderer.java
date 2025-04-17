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

import static pitheguy.countycolor.render.util.RenderConst.OUTLINE_THICKNESS;
import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public abstract class RegionRenderer implements Disposable {
    private static final ExecutorService SHAPE_LOAD_EXECUTOR = Executors.newCachedThreadPool();

    private final Future<?> future;
    protected final ShapeRenderer shapeRenderer = new ShapeRenderer();
    protected final Map<List<Vector2>, ShortArray> triangles = new HashMap<>();

    public RegionRenderer() {
        future = loadShapesAsync();
    }

    protected void renderRegion(Collection<PolygonCollection> subregions, OrthographicCamera camera, boolean thick, boolean scaleThickness) {
        ensureLoadingFinished();
        shapeRenderer.begin(thick ? ShapeRenderer.ShapeType.Filled : ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);
        for (PolygonCollection subregion : subregions) {
            if (!subregion.isVisibleToCamera(camera)) continue;
            if (thick) renderThickSubregionOutline(subregion, scaleThickness ? OUTLINE_THICKNESS * camera.zoom : OUTLINE_THICKNESS);
            else renderSubregionOutline(subregion);
        }
        shapeRenderer.end();
    }

    protected void renderBackground() {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    protected void renderSubregionOutline(PolygonCollection subregion) {
        for (List<Vector2> points : subregion.getPolygons()) {
            for (int i = 0; i < points.size() - 1; i++) {
                Vector2 point = points.get(i);
                Vector2 endPoint = points.get(i + 1);
                shapeRenderer.line(point.x * RENDER_SIZE / 2f, point.y * RENDER_SIZE / 2f, endPoint.x * RENDER_SIZE / 2f, endPoint.y * RENDER_SIZE / 2f);
            }
        }
    }

    protected void renderThickSubregionOutline(PolygonCollection subregion, float thickness) {
        for (List<Vector2> points : subregion.getPolygons())
            RenderUtil.drawThickPolyline(shapeRenderer, points, thickness);
    }

    protected void fillSubregion(PolygonCollection subregion, Color color) {
        ensureLoadingFinished();
        shapeRenderer.setColor(color);
        for (List<Vector2> points : subregion.getPolygons())
            RenderUtil.renderFilledPolygon(shapeRenderer, points, triangles.computeIfAbsent(points, RenderUtil::triangulate), 1);
    }

    protected void updateCamera(OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
    }

    private Future<?> loadShapesAsync() {
        return SHAPE_LOAD_EXECUTOR.submit(this::loadShapes);
    }

    protected abstract void loadShapes();

    public static PolygonCollection loadSubregion(JsonValue subregion) {
        JsonValue geometry = subregion.get("geometry");
        String type = geometry.getString("type");
        JsonValue coordinates = geometry.get("coordinates");
        List<JsonValue> shapesJson;
        switch (type) {
            case "Polygon":
                shapesJson = List.of(coordinates);
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

    public void ensureLoadingFinished() {
        Object result = Util.getFutureValue(future);
        if (result instanceof Throwable) throw new RuntimeException("Failed to load shapes", (Throwable) result);
    }

    public boolean isDoneLoading() {
        return future.isDone();
    }


    protected static Map<String, PolygonCollection> relativize(Map<String, PolygonCollection> shapes) {
        return relativize(shapes, shapes);
    }

    protected static Map<String, PolygonCollection> relativize(Map<String, PolygonCollection> shapes, Map<String, PolygonCollection> reference) {
        float minX = (float) reference.values().stream().mapToDouble(PolygonCollection::getMinX).min().getAsDouble();
        float minY = (float) reference.values().stream().mapToDouble(PolygonCollection::getMinY).min().getAsDouble();
        float maxX = (float) reference.values().stream().mapToDouble(PolygonCollection::getMaxX).max().getAsDouble();
        float maxY = (float) reference.values().stream().mapToDouble(PolygonCollection::getMaxY).max().getAsDouble();
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

    public Zoom getTargetZoom(PolygonCollection subregion) {
        float minX = subregion.getMinX();
        float minY = subregion.getMinY();
        float maxX = subregion.getMaxX();
        float maxY = subregion.getMaxY();
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

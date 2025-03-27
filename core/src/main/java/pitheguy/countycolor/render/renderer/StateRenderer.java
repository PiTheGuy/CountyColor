package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;
import pitheguy.countycolor.coloring.CountyData;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.Zoom;
import pitheguy.countycolor.render.util.RenderConst;
import pitheguy.countycolor.render.util.RenderUtil;

import java.util.*;
import java.util.concurrent.*;

public class StateRenderer {
    private static final Map<String, String> ID_TO_STATE = new HashMap<>();
    private static final Map<String, String> STATE_TO_ID = new HashMap<>();
    static {
        initStateIdMap();
    }
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final Future<Map<String, PolygonCollection>> shapesFuture;
    private final Map<List<Vector2>, ShortArray> triangles = new HashMap<>();
    private Map<String, PolygonCollection> shapes = null;

    public StateRenderer(String state) {
        this.shapesFuture = loadState(state);
    }

    public void renderState(OrthographicCamera camera, CountyData countyData) {
        ensureLoadingFinished();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(-RenderConst.RENDER_SIZE, -RenderConst.RENDER_SIZE, RenderConst.RENDER_SIZE * 2, RenderConst.RENDER_SIZE * 2); // White background
        shapeRenderer.end();
        ShapeRenderer lineRenderer = new ShapeRenderer();
        lineRenderer.setProjectionMatrix(camera.combined);
        lineRenderer.setColor(Color.BLACK);
        lineRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (String county : shapes.keySet()) {
            PolygonCollection countyPolygons = shapes.get(county);
            if (countyData.get(county).isCompleted()) {
                shapeRenderer.setColor(countyData.get(county).mapColor().getColor());
                for (List<Vector2> points : countyPolygons.getPolygons())
                    RenderUtil.renderFilledPolygon(shapeRenderer, points, getTriangles(points), 1);
            }
            shapeRenderer.setColor(Color.BLACK);
            if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
                for (List<Vector2> points : countyPolygons.getPolygons()) {
                    RenderUtil.drawThickPolyline(shapeRenderer, points, RenderConst.OUTLINE_THICKNESS * camera.zoom, RenderConst.RENDER_SIZE);
                }
            } else renderMobile(lineRenderer, countyPolygons);
        }
        lineRenderer.end();
        lineRenderer.dispose();
        shapeRenderer.end();
    }

    private void renderMobile(ShapeRenderer lineRenderer, PolygonCollection countyPolygons) {
        for (List<Vector2> points : countyPolygons.getPolygons()) {
            for (int i = 0; i < points.size() - 1; i++) {
                Vector2 point = points.get(i).cpy().scl(RenderConst.RENDER_SIZE / 2f);
                Vector2 endPoint = points.get(i + 1).cpy().scl(RenderConst.RENDER_SIZE / 2f);
                lineRenderer.line(point.x, point.y, endPoint.x, endPoint.y);
            }
        }
    }

    private ShortArray getTriangles(List<Vector2> points) {
        return triangles.computeIfAbsent(points, RenderUtil::triangulate);
    }

    private void ensureLoadingFinished() {
        if (shapes == null) {
            try {
                shapes = shapesFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void initStateIdMap() {
        String[] mappings = Gdx.files.internal("state_ids.txt").readString().split("\n");
        for (String mapping : mappings) {
            String[] parts = mapping.split("=");
            ID_TO_STATE.put(parts[1], parts[0]);
            STATE_TO_ID.put(parts[0], parts[1]);
        }
    }

    public static String getIdForState(String state) {
        return STATE_TO_ID.get(state);
    }

    public static String getStateFromId(String id) {
        return ID_TO_STATE.get(id);
    }

    private Future<Map<String, PolygonCollection>> loadState(String state) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal("counties.json"));
            JsonValue array = root.get("features");

            Map<String, List<List<Vector2>>> shapes = new HashMap<>();

            for (JsonValue country : array) {
                JsonValue properties = country.get("properties");
                String stateId = properties.getString("STATEFP");

                if (!state.equals(ID_TO_STATE.get(stateId))) continue;

                JsonValue geometry = country.get("geometry");
                String type = geometry.getString("type");
                JsonValue coordinates = geometry.get("coordinates");

                List<JsonValue> shapesJson = switch (type) {
                    case "Polygon" -> List.of(coordinates);
                    case "MultiPolygon" -> {
                        List<JsonValue> polys = new ArrayList<>();
                        for (JsonValue polygon : coordinates) polys.add(polygon);
                        yield polys;
                    }
                    default -> throw new IllegalStateException("Unexpected type: " + type);
                };

                List<List<Vector2>> polygons = new ArrayList<>();

                for (JsonValue arr : shapesJson) {
                    JsonValue outer = arr.get(0); // Use outer ring
                    List<Vector2> points = new ArrayList<>();
                    for (JsonValue point : outer) {
                        float lat = point.getFloat(0);
                        float lng = point.getFloat(1);
                        points.add(new Vector2(lat, lng));
                    }
                    polygons.add(points);
                }

                shapes.put(properties.getString("NAME"), polygons);
            }

            float minX = (float) shapes.values().stream()
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .mapToDouble(v -> v.x)
                .min()
                .getAsDouble();
            float minY = (float) shapes.values().stream()
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .mapToDouble(v -> v.y)
                .min()
                .getAsDouble();
            float maxX = (float) shapes.values().stream()
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .mapToDouble(v -> v.x)
                .max()
                .getAsDouble();
            float maxY = (float) shapes.values().stream()
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .mapToDouble(v -> v.y)
                .max()
                .getAsDouble();

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
            Map<String, PolygonCollection> relativeShapes = new HashMap<>();
            for (Map.Entry<String, List<List<Vector2>>> entry : shapes.entrySet()) {
                List<List<Vector2>> county = new ArrayList<>();
                for (List<Vector2> points : entry.getValue()) {
                    List<Vector2> relativePoints = new ArrayList<>();
                    for (Vector2 point : points) relativePoints.add(relativize(point, minX, maxX, minY, maxY));
                    county.add(relativePoints);
                }
                relativeShapes.put(entry.getKey(), new PolygonCollection(county));
            }
            return relativeShapes;
        });
    }

    private static Vector2 relativize(Vector2 point, float minX, float maxX, float minY, float maxY) {
        float xDiff = maxX - minX;
        float yDiff = maxY - minY;
        float x = ((point.x - minX) / xDiff) * 2f - 1f;
        float y = ((point.y - minY) / yDiff) * 2f - 1f;
        return new Vector2(x, y);
    }

    public String getCountyAtCoords(Vector2 coordinate) {
        for (Map.Entry<String, PolygonCollection> entry : shapes.entrySet()) {
            PolygonCollection county = entry.getValue();
            if (!county.boundsCheck(coordinate)) continue;
            for (List<Vector2> polygon : county.getPolygons()) {
                List<Vector2> copy = new ArrayList<>(polygon);
                copy.replaceAll(v -> v.cpy().scl(RenderConst.RENDER_SIZE / 2f));
                if (RenderUtil.pointInPolygon(coordinate, copy)) return entry.getKey();
            }
        }
        return "";
    }

    public Zoom getTargetZoom(String county) {
        PolygonCollection countyPolygons = shapes.get(county);
        float minX = countyPolygons.getMinX();
        float minY = countyPolygons.getMinY();
        float maxX = countyPolygons.getMaxX();
        float maxY = countyPolygons.getMaxY();
        float xRange = (maxX - minX) / 2;
        float yRange = (maxY - minY) / 2;
        float zoom = Math.max(xRange, yRange) * RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float xCenter = (minX + maxX) / 4f * RenderConst.RENDER_SIZE;
        float yCenter = (minY + maxY) / 4f * RenderConst.RENDER_SIZE;
        return new Zoom(new Vector2(xCenter, yCenter), zoom);
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}

package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ShortArray;
import com.google.gson.*;
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

    public void renderState(OrthographicCamera camera, List<String> completedCounties) {
        ensureLoadingFinished();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(-RenderConst.RENDER_SIZE, -RenderConst.RENDER_SIZE, RenderConst.RENDER_SIZE * 2, RenderConst.RENDER_SIZE * 2); // White background
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLACK);
        for (String county : shapes.keySet()) {
            PolygonCollection countyPolygons = shapes.get(county);
            if (completedCounties.contains(county)) for (List<Vector2> points : countyPolygons.getPolygons())
                RenderUtil.renderFilledPolygon(shapeRenderer, points, getTriangles(points), 1);
            else for (List<Vector2> points : countyPolygons.getPolygons()) {
                List<Vector2> pointsCopy = new ArrayList<>(points);
                pointsCopy.replaceAll(Vector2::cpy);
                RenderUtil.drawThickPolyline(shapeRenderer, pointsCopy, RenderConst.OUTLINE_THICKNESS * camera.zoom, RenderConst.RENDER_SIZE);
            }
        }
        shapeRenderer.end();
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
            JsonArray array = JsonParser.parseReader(Gdx.files.internal("counties.json").reader())
                .getAsJsonObject().getAsJsonArray("features");
            Map<String, List<List<Vector2>>> shapes = new HashMap<>();

            for (JsonElement countryElement : array) {
                JsonObject country = countryElement.getAsJsonObject();
                JsonObject properties = country.getAsJsonObject("properties");
                String stateId = properties.get("STATEFP").getAsString();
                if (!state.equals(ID_TO_STATE.get(stateId))) continue;

                JsonObject geometry = country.getAsJsonObject("geometry");
                String type = geometry.get("type").getAsString();
                JsonArray coordinates = geometry.getAsJsonArray("coordinates");

                List<JsonArray> shapesJson = switch (type) {
                    case "Polygon" -> List.of(coordinates);
                    case "MultiPolygon" -> coordinates.asList().stream().map(JsonElement::getAsJsonArray).toList();
                    default -> throw new IllegalStateException("Unexpected type: " + type);
                };

                List<List<Vector2>> polygons = new ArrayList<>();

                for (JsonArray arr : shapesJson) {
                    List<Vector2> points = new ArrayList<>();
                    for (JsonElement pointElement : arr.get(0).getAsJsonArray()) {
                        JsonArray point = pointElement.getAsJsonArray();
                        double lat = point.get(0).getAsDouble();
                        double lng = point.get(1).getAsDouble();
                        points.add(new Vector2((float) lat, (float) lng));
                    }
                    polygons.add(points);
                }
                shapes.put(properties.get("NAME").getAsString(), polygons);
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
        float zoom = Math.max(xRange, yRange);
        float xCenter = (minX + maxX) / 4f * RenderConst.RENDER_SIZE;
        float yCenter = (minY + maxY) / 4f * RenderConst.RENDER_SIZE;
        return new Zoom(new Vector2(xCenter, yCenter), zoom);
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}

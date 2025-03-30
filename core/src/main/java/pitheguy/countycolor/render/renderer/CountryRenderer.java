package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;
import pitheguy.countycolor.gui.screens.CountryScreen;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.Zoom;
import pitheguy.countycolor.render.util.RenderConst;
import pitheguy.countycolor.render.util.RenderUtil;
import pitheguy.countycolor.util.Util;

import java.util.*;
import java.util.Collections;
import java.util.concurrent.Future;

public class CountryRenderer {
    private static final List<String> FILTERED_STATES = List.of(
        "Guam",
        "Puerto Rico",
        "American Samoa",
        "United States Virgin Islands",
        "Commonwealth of the Northern Mariana Islands",
        "District of Columbia"
    );

    public static final List<String> RENDERED_SEPARATELY = List.of("Alaska", "Hawaii");

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final Map<String, PolygonCollection> shapes;
    private final Map<List<Vector2>, ShortArray> triangles = new HashMap<>();

    public CountryRenderer() {
        this.shapes = loadShapes();
    }

    public void renderCountry(OrthographicCamera camera, Future<Map<String, Integer>> completionCounts) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(-RenderConst.RENDER_SIZE, -RenderConst.RENDER_SIZE, RenderConst.RENDER_SIZE * 2, RenderConst.RENDER_SIZE * 2); // White background
        shapeRenderer.end();
        List<String> completedStates = new ArrayList<>();
        for (String state : shapes.keySet()) {
            boolean completed = completionCounts.isDone() && Objects.equals(Util.getFutureValue(completionCounts).getOrDefault(state, 0), CountryScreen.COUNTY_COUNTS.get(state));
            if (completed) completedStates.add(state);
        }
        if (!completedStates.isEmpty()) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.GREEN);
            for (String stateName : completedStates) {
                PolygonCollection state = shapes.get(stateName);
                for (List<Vector2> points : state.getPolygons()) {
                    RenderUtil.renderFilledPolygon(shapeRenderer, points, triangles.computeIfAbsent(points, RenderUtil::triangulate), 1);
                }
            }
            shapeRenderer.end();
        }
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (String stateName : shapes.keySet()) {
            PolygonCollection state = shapes.get(stateName);
            for (List<Vector2> points : state.getPolygons()) {
                for (int i = 0; i < points.size() - 1; i++) {
                    Vector2 point = points.get(i);
                    Vector2 endPoint = points.get(i + 1);
                    shapeRenderer.line(point.x * RenderConst.RENDER_SIZE / 2f, point.y * RenderConst.RENDER_SIZE / 2f, endPoint.x * RenderConst.RENDER_SIZE / 2f, endPoint.y * RenderConst.RENDER_SIZE / 2f);
                }
            }
        }
        shapeRenderer.end();
    }

    private Map<String, PolygonCollection> loadShapes() {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(Gdx.files.internal("metadata/states.geojson"));
        JsonValue array = root.get("features");

        Map<String, PolygonCollection> shapes = new HashMap<>();
        Map<String, PolygonCollection> renderedSeparately = new HashMap<>();

        for (JsonValue country : array) {
            JsonValue properties = country.get("properties");
            String stateName = properties.getString("NAME");

            if (FILTERED_STATES.contains(stateName)) continue;

            JsonValue geometry = country.get("geometry");
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
            if (stateName.equals("Alaska")) RenderUtil.fixRollover(polygons);
            if (RENDERED_SEPARATELY.contains(stateName)) renderedSeparately.put(stateName, new PolygonCollection(polygons));
            else shapes.put(stateName, new PolygonCollection(polygons));
        }
        float minX = (float) shapes.values().stream().mapToDouble(PolygonCollection::getMinX).min().getAsDouble();
        float minY = (float) shapes.values().stream().mapToDouble(PolygonCollection::getMinY).min().getAsDouble();
        float maxX = (float) shapes.values().stream().mapToDouble(PolygonCollection::getMaxX).max().getAsDouble();
        float maxY = (float) shapes.values().stream().mapToDouble(PolygonCollection::getMaxY).max().getAsDouble();
        Map<String, PolygonCollection> relativeShapes = new HashMap<>();
        for (Map.Entry<String, PolygonCollection> entry : shapes.entrySet()) {
            List<List<Vector2>> county = new ArrayList<>();
            for (List<Vector2> points : entry.getValue().getPolygons()) {
                List<Vector2> relativePoints = new ArrayList<>();
                for (Vector2 point : points) relativePoints.add(relativize(point, minX, maxX, minY, maxY));
                county.add(relativePoints);
            }
            relativeShapes.put(entry.getKey(), new PolygonCollection(county));
        }
        addRenderedSeparately(relativeShapes, renderedSeparately);
        return relativeShapes;
    }

    private void addRenderedSeparately(Map<String, PolygonCollection> map, Map<String, PolygonCollection> renderedSeparately) {
        int i = 0;
        List<Map.Entry<String, PolygonCollection>> entries = new ArrayList<>(renderedSeparately.entrySet());
        Collections.reverse(entries);
        for (Map.Entry<String, PolygonCollection> entry : entries) {
            float startX = -0.8f + 0.3f * i;
            PolygonCollection polygons = entry.getValue();
            List<List<Vector2>> relativeShapes = new ArrayList<>();
            for (List<Vector2> points : polygons.getPolygons()) {
                List<Vector2> relativePoints = new ArrayList<>();
                for (Vector2 point : points) {
                    Vector2 relativized = relativize(point, polygons.getMinX(), polygons.getMaxX(), polygons.getMinY(), polygons.getMaxY());
                    relativized.scl(0.25f).add(startX, -0.4f);
                    relativePoints.add(relativized);
                }
                relativeShapes.add(relativePoints);
            }
            map.put(entry.getKey(), new PolygonCollection(relativeShapes));
            i++;
        }
    }

    private static Vector2 relativize(Vector2 point, float minX, float maxX, float minY, float maxY) {
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

    public String getStateAtCoords(Vector2 coordinate) {
        for (Map.Entry<String, PolygonCollection> entry : shapes.entrySet()) {
            PolygonCollection state = entry.getValue();
            if (!state.boundsCheck(coordinate)) continue;
            for (List<Vector2> polygon : state.getPolygons())
                if (RenderUtil.pointInPolygon(coordinate.cpy().scl(2f / RenderConst.RENDER_SIZE), polygon))
                    return entry.getKey();
        }
        return "";
    }

    public void dispose() {
        shapeRenderer.dispose();
    }

    public Zoom getTargetZoom(String state) {
        PolygonCollection statePolygons = shapes.get(state);
        float minX = statePolygons.getMinX();
        float minY = statePolygons.getMinY();
        float maxX = statePolygons.getMaxX();
        float maxY = statePolygons.getMaxY();
        float xRange = (maxX - minX) / 2;
        float yRange = (maxY - minY) / 2;
        float zoom = Math.max(xRange, yRange) * RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float xCenter = (minX + maxX) / 4f * RenderConst.RENDER_SIZE;
        float yCenter = (minY + maxY) / 4f * RenderConst.RENDER_SIZE;
        return new Zoom(new Vector2(xCenter, yCenter), zoom);
    }
}

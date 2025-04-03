package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.gui.screens.CountryScreen;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.util.RenderUtil;
import pitheguy.countycolor.util.Util;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Predicate;

public class CountryRenderer extends RegionRenderer {
    private static final List<String> FILTERED_STATES = List.of(
        "Guam",
        "Puerto Rico",
        "American Samoa",
        "United States Virgin Islands",
        "Commonwealth of the Northern Mariana Islands",
        "District of Columbia"
    );

    public static final List<String> RENDERED_SEPARATELY = List.of("Alaska", "Hawaii");

    public CountryRenderer() {
        super("metadata/states.geojson", properties -> !FILTERED_STATES.contains(properties.getString("NAME")));
    }

    public void renderCountry(OrthographicCamera camera, Future<Map<String, Integer>> completionCounts) {
        ensureLoadingFinished();
        updateCamera(camera);
        renderBackground();
        List<String> completedStates = new ArrayList<>();
        for (String state : shapes.keySet()) {
            boolean completed = completionCounts.isDone() && Objects.equals(Util.getFutureValue(completionCounts).getOrDefault(state, 0), CountryScreen.COUNTY_COUNTS.get(state));
            if (completed) completedStates.add(state);
        }
        if (!completedStates.isEmpty()) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (String stateName : completedStates) fillSubregion(stateName, Color.GREEN);
            shapeRenderer.end();
        }
        renderRegion(camera, false, true);
    }

    @Override
    protected Map<String, PolygonCollection> loadShapes(String sourceFilePath, Predicate<JsonValue> predicate, String duplicatePreventionKey) {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(Gdx.files.internal(sourceFilePath));
        JsonValue array = root.get("features");
        Map<String, PolygonCollection> shapes = new HashMap<>();
        Map<String, PolygonCollection> renderedSeparately = new HashMap<>();
        for (JsonValue subregion : array) {
            JsonValue properties = subregion.get("properties");
            if (!predicate.test(properties)) continue;
            String subregionName = properties.getString("NAME");
            PolygonCollection polygons = loadSubregion(subregion);
            if (subregionName.equals("Alaska")) RenderUtil.fixRollover(polygons);
            if (RENDERED_SEPARATELY.contains(subregionName)) renderedSeparately.put(subregionName, polygons);
            else shapes.put(subregionName, polygons);
        }
        Map<String, PolygonCollection> relativeShapes = relativize(shapes);
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
                    Vector2 relativized = relativizePoint(point, polygons.getMinX(), polygons.getMaxX(), polygons.getMinY(), polygons.getMaxY());
                    relativized.scl(0.25f).add(startX, -0.4f);
                    relativePoints.add(relativized);
                }
                relativeShapes.add(relativePoints);
            }
            map.put(entry.getKey(), new PolygonCollection(relativeShapes));
            i++;
        }
    }
}

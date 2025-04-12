package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.coloring.CountyData;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.metadata.StateBorders;
import pitheguy.countycolor.options.Options;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.util.*;
import pitheguy.countycolor.util.Util;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static pitheguy.countycolor.render.util.RenderConst.OUTLINE_THICKNESS;

public class StateRenderer extends CountyLevelRenderer {
    private static final Map<String, String> STATE_TO_ID = new HashMap<>();
    private static final Map<String, String> ID_TO_STATE = new HashMap<>();
    static {
        initStateIdMap();
    }
    private final BooleanSupplier useCachedTexture;
    private final BooleanSupplier renderHoveringCounty;
    private final Future<Map<String, Map<String, MapColor>>> completedCounties;
    private final String state;
    private final RenderCachingHelper cachingHelper;
    private Map<String, PolygonCollection> auxiliaryShapes;


    public StateRenderer(String state, BooleanSupplier useCachedTexture, BooleanSupplier renderHoveringCounty, Future<Map<String, Map<String, MapColor>>> completedCounties) {
        super("metadata/counties.json", properties -> properties.getString("STATEFP").equals(STATE_TO_ID.get(state)));
        this.state = state;
        this.useCachedTexture = useCachedTexture;
        this.renderHoveringCounty = renderHoveringCounty;
        this.completedCounties = completedCounties;
        this.cachingHelper = new RenderCachingHelper();
    }

    public void renderState(OrthographicCamera camera, CountyData countyData) {
        renderBackground();
        shapeRenderer.setProjectionMatrix(camera.combined);
        if (useCachedTexture.getAsBoolean()) cachingHelper.render(camera, cam -> renderStateInternal(cam, countyData));
        else renderStateInternal(camera, countyData);
        if (renderHoveringCounty.getAsBoolean()) {
            String hoveringCounty = getSubregionAtCoords(RenderUtil.getMouseWorldCoords(camera));
            if (hoveringCounty != null && !countyData.get(hoveringCounty).isCompleted()) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                fillSubregion(hoveringCounty, new Color(0.95f, 0.95f, 0.95f, 1f));
                shapeRenderer.setColor(Color.BLACK);
                renderThickSubregionOutline(hoveringCounty, OUTLINE_THICKNESS * camera.zoom);
                shapeRenderer.end();
                renderIndependentCities(camera, true, true, name -> !name.equals(hoveringCounty) && shapes.get(name).boundingBoxOverlaps(shapes.get(hoveringCounty)));
            }
        }
    }

    private void renderStateInternal(OrthographicCamera camera, CountyData countyData) {
        ensureLoadingFinished();
        updateCamera(camera);
        renderBackground();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (String county : shapes.keySet())
            if (shapes.get(county).isVisibleToCamera(camera) && countyData.get(county).isCompleted())
                fillSubregion(county, countyData.get(county).mapColor().getColor());
        shapeRenderer.end();
        renderRegion(camera, true, true);
        if (Options.NEIGHBOR_BORDER_COLORS.get()) renderNeighborBorderColors(camera);
    }

    private void renderNeighborBorderColors(OrthographicCamera camera) {
        if (!completedCounties.isDone()) return;
        Map<String, Map<String, MapColor>> completedCountiesMap = Util.getFutureValue(completedCounties);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (String inStateCounty : shapes.keySet()) {
            PolygonCollection inPoly = auxiliaryShapes.get(getIdForState(state) + inStateCounty);
            for (Map.Entry<String, PolygonCollection> entry : auxiliaryShapes.entrySet()) {
                String outCounty = entry.getKey();
                PolygonCollection outPoly = entry.getValue();
                String outState = getStateFromId(outCounty.substring(0, 2));
                if (outState.equals(state)) continue;
                if (!completedCountiesMap.containsKey(outState)) continue;
                if (inPoly.isAdjacentTo(outPoly) && completedCountiesMap.get(outState).containsKey(outCounty.substring(2))) {
                    drawSharedBorder(inPoly, outPoly, completedCountiesMap.get(outState).get(outCounty.substring(2)), camera.zoom);
                }
            }
        }
        shapeRenderer.end();
    }

    private void drawSharedBorder(PolygonCollection a, PolygonCollection b, MapColor color, float zoom) {
        List<List<Vector2>> sharedEdges = a.getSharedEdges(b);
        shapeRenderer.setColor(color.getColor());
        for (List<Vector2> edge : sharedEdges)
            RenderUtil.drawThickPolyline(shapeRenderer, edge, OUTLINE_THICKNESS * zoom * 2, false);
    }


    public void invalidateCache() {
        cachingHelper.invalidateCache();
    }

    public List<String> getBorderingCounties(String county) {
        PolygonCollection polygons = shapes.get(county);
        List<String> borderingCounties = new ArrayList<>();
        for (Map.Entry<String, PolygonCollection> entry : shapes.entrySet()) {
            if (entry.getKey().equals(county)) continue;
            PolygonCollection other = entry.getValue();
            if (polygons.isAdjacentTo(other)) borderingCounties.add(entry.getKey());
        }
        return borderingCounties;
    }

    private static void initStateIdMap() {
        String[] mappings = Gdx.files.internal("metadata/state_ids.txt").readString().split("\n");
        for (String mapping : mappings) {
            String[] parts = mapping.split("=");
            STATE_TO_ID.put(parts[0], parts[1]);
            ID_TO_STATE.put(parts[1], parts[0]);
        }
    }

    public static String getIdForState(String state) {
        return STATE_TO_ID.get(state);
    }

    public static String getStateFromId(String id) {
        return ID_TO_STATE.get(id);
    }

    @Override
    protected Map<String, PolygonCollection> loadShapes(String sourceFilePath, Predicate<JsonValue> predicate, String duplicatePreventionKey) {
        List<String> borderingStates = StateBorders.getBorderingStates(state);
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(Gdx.files.internal(sourceFilePath));
        JsonValue array = root.get("features");
        Map<String, PolygonCollection> shapes = new HashMap<>();
        auxiliaryShapes = new HashMap<>();
        for (JsonValue subregion : array) {
            JsonValue properties = subregion.get("properties");
            String stateId = properties.getString("STATEFP");
            String state = getStateFromId(stateId);
            if (!state.equals(this.state) && !borderingStates.contains(state)) continue;
            String subregionName = properties.getString("NAME");
            PolygonCollection polygons = loadSubregion(subregion);
            if (state.equals(this.state)) {
                shapes.put(subregionName, polygons);
                postProcessJson(subregion);
            }
            auxiliaryShapes.put(stateId + subregionName, polygons);
        }
        postProcessShapes(shapes);
        auxiliaryShapes = relativize(auxiliaryShapes, shapes);
        return relativize(shapes);
    }

    @Override
    protected void postProcessShapes(Map<String, PolygonCollection> shapes) {
        if (state.equals("Alaska")) for (PolygonCollection polygons : shapes.values()) RenderUtil.fixRollover(polygons);
    }

    public void dispose() {
        super.dispose();
        cachingHelper.dispose();
    }
}

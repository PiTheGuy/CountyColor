package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import pitheguy.countycolor.coloring.CountyCompletionData;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.metadata.CountyData;
import pitheguy.countycolor.metadata.StateBorders;
import pitheguy.countycolor.options.Options;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.util.RenderCachingHelper;
import pitheguy.countycolor.render.util.RenderUtil;
import pitheguy.countycolor.util.Util;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import static pitheguy.countycolor.render.util.RenderConst.OUTLINE_THICKNESS;

public class StateRenderer extends CountyLevelRenderer {
    private final BooleanSupplier useCachedTexture;
    private final BooleanSupplier renderHoveringCounty;
    private final Future<Map<String, Map<String, MapColor>>> completedCounties;
    private final String state;
    private final RenderCachingHelper cachingHelper;
    private Map<String, CountyData.County> auxiliaryCounties;
    private Map<List<List<Vector2>>, MapColor> neighborBorderColors;

    public StateRenderer(String state, BooleanSupplier useCachedTexture, BooleanSupplier renderHoveringCounty, Future<Map<String, Map<String, MapColor>>> completedCounties) {
        this.state = state;
        this.useCachedTexture = useCachedTexture;
        this.renderHoveringCounty = renderHoveringCounty;
        this.completedCounties = completedCounties;
        this.cachingHelper = new RenderCachingHelper();
    }

    public void renderState(OrthographicCamera camera, CountyCompletionData countyCompletionData) {
        renderBackground();
        shapeRenderer.setProjectionMatrix(camera.combined);
        if (useCachedTexture.getAsBoolean()) cachingHelper.render(camera, cam -> renderStateInternal(cam, countyCompletionData));
        else renderStateInternal(camera, countyCompletionData);
        if (renderHoveringCounty.getAsBoolean()) {
            CountyData.County hoveringCounty = getCountyAtCoords(RenderUtil.getMouseWorldCoords(camera));
            if (hoveringCounty != null && !countyCompletionData.get(hoveringCounty.getName()).isCompleted()) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                fillSubregion(hoveringCounty.getPolygons(), new Color(0.95f, 0.95f, 0.95f, 1f));
                shapeRenderer.setColor(Color.BLACK);
                renderThickSubregionOutline(hoveringCounty.getPolygons(), OUTLINE_THICKNESS * camera.zoom);
                shapeRenderer.end();
                renderIndependentCities(camera, true, true, county -> !county.equals(hoveringCounty) && county.getPolygons().boundingBoxOverlaps(hoveringCounty.getPolygons()));
            }
        }
    }

    private void renderStateInternal(OrthographicCamera camera, CountyCompletionData countyCompletionData) {
        ensureLoadingFinished();
        updateCamera(camera);
        renderBackground();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (CountyData.County county : counties.values())
            if (county.getPolygons().isVisibleToCamera(camera) && countyCompletionData.get(county.getName()).isCompleted())
                fillSubregion(county.getPolygons(), countyCompletionData.get(county.getName()).mapColor().getColor());
        shapeRenderer.end();
        renderRegion(camera, true, true);
        if (Options.NEIGHBOR_BORDER_COLORS.get()) renderNeighborBorderColors(camera);
    }

    private void renderNeighborBorderColors(OrthographicCamera camera) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Map.Entry<List<List<Vector2>>, MapColor> entry : neighborBorderColors.entrySet()) {
            List<List<Vector2>> sharedEdges = entry.getKey();
            shapeRenderer.setColor(entry.getValue().getColor());
            for (List<Vector2> edge : sharedEdges)
                RenderUtil.drawThickPolyline(shapeRenderer, edge, OUTLINE_THICKNESS * camera.zoom * 2, false);
        }
        shapeRenderer.end();
    }

    private void loadNeighborBorderColors() {
        Map<String, Map<String, MapColor>> completedCountiesMap = Util.getFutureValue(completedCounties);
        neighborBorderColors = new HashMap<>();
        for (String inStateCounty : counties.keySet()) {
            PolygonCollection inPoly = auxiliaryCounties.get(inStateCounty + "," + state).getPolygons();
            for (Map.Entry<String, CountyData.County> entry : auxiliaryCounties.entrySet()) {
                String outCounty = entry.getKey();
                PolygonCollection outPoly = entry.getValue().getPolygons();
                String outCountyName = outCounty.split(",")[0];
                String outState = outCounty.split(",")[1];
                if (outState.equals(state)) continue;
                if (!completedCountiesMap.containsKey(outState)) continue;
                if (inPoly.isAdjacentTo(outPoly) && completedCountiesMap.get(outState).containsKey(outCountyName)) {
                    List<List<Vector2>> sharedEdges = inPoly.getSharedEdges(outPoly);
                    neighborBorderColors.put(sharedEdges, completedCountiesMap.get(outState).get(outCountyName));
                }
            }
        }
    }

    public void invalidateCache() {
        cachingHelper.invalidateCache();
    }

    public List<String> getBorderingCounties(CountyData.County county) {
        List<String> borderingCounties = new ArrayList<>();
        for (Map.Entry<String, CountyData.County> entry : counties.entrySet()) {
            if (entry.getValue().equals(county)) continue;
            CountyData.County other = entry.getValue();
            if (county.getPolygons().isAdjacentTo(other.getPolygons())) borderingCounties.add(entry.getKey());
        }
        return borderingCounties;
    }

    @Override
    protected void loadShapes() {
        List<String> borderingStates = StateBorders.getBorderingStates(state);
        Map<String, CountyData.County> currentStateCounties = CountyData.getCountiesForState(state);
        List<Map<String, CountyData.County>> borderingStateCounties = borderingStates.stream().map(CountyData::getCountiesForState).collect(Collectors.toList());
        borderingStateCounties.add(currentStateCounties);
        auxiliaryCounties = new HashMap<>();
        borderingStateCounties.stream().flatMap(map -> map.values().stream()).forEach(county -> auxiliaryCounties.put(county.getName() + "," + county.getState(), county));
        counties = rel(currentStateCounties);
        auxiliaryCounties = rel(auxiliaryCounties, currentStateCounties);
        if (Options.NEIGHBOR_BORDER_COLORS.get()) loadNeighborBorderColors();
    }

    public static Map<String, CountyData.County> rel(Map<String, CountyData.County> counties) {
        return rel(counties, counties);
    }

    public static Map<String, CountyData.County> rel(Map<String, CountyData.County> counties, Map<String, CountyData.County> reference) {
        Map<String, PolygonCollection> shapes = counties.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getPolygons()));
        Map<String, PolygonCollection> refShapes = reference.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getPolygons()));
        Map<String, PolygonCollection> newShapes = relativize(shapes, refShapes);
        return counties.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().withPolygons(newShapes.get(entry.getKey()))));
    }

    public void dispose() {
        super.dispose();
        cachingHelper.dispose();
    }
}

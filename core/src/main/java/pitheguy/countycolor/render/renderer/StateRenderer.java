package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import pitheguy.countycolor.coloring.CountyData;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.util.*;

import java.util.*;
import java.util.function.BooleanSupplier;

public class StateRenderer extends RegionRenderer {
    private static final Map<String, String> STATE_TO_ID = new HashMap<>();
    private static final Map<String, String> ID_TO_STATE = new HashMap<>();
    static {
        initStateIdMap();
    }
    private final BooleanSupplier useCachedTexture;
    private final BooleanSupplier renderHoveringCounty;
    private final String state;
    private final RenderCachingHelper cachingHelper;


    public StateRenderer(String state, BooleanSupplier useCachedTexture, BooleanSupplier renderHoveringCounty) {
        super("metadata/counties.json", properties -> properties.getString("STATEFP").equals(STATE_TO_ID.get(state)));
        this.state = state;
        this.useCachedTexture = useCachedTexture;
        this.renderHoveringCounty = renderHoveringCounty;
        this.cachingHelper = new RenderCachingHelper();
    }

    public void renderState(OrthographicCamera camera, CountyData countyData) {
        renderBackground();
        shapeRenderer.setProjectionMatrix(camera.combined);
        if (useCachedTexture.getAsBoolean()) cachingHelper.render(camera, cam -> renderStateInternal(cam, countyData));
        else renderStateInternal(camera, countyData);
        if (renderHoveringCounty.getAsBoolean()) {
            String hoveringCounty = getSubregionAtCoords(RenderUtil.getMouseWorldCoords(camera));
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            if (hoveringCounty != null && !countyData.get(hoveringCounty).isCompleted()) {
                fillSubregion(hoveringCounty, new Color(0.95f, 0.95f, 0.95f, 1f));
                shapeRenderer.setColor(Color.BLACK);
                renderThickSubregionOutline(hoveringCounty, RenderConst.OUTLINE_THICKNESS * camera.zoom);
            }
            shapeRenderer.end();
        }
    }

    private void renderStateInternal(OrthographicCamera camera, CountyData countyData) {
        ensureLoadingFinished();
        updateCamera(camera);
        renderBackground();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (String county : shapes.keySet())
            if (countyData.get(county).isCompleted())
                fillSubregion(county, countyData.get(county).mapColor().getColor());
        shapeRenderer.end();
        renderRegion(camera, true, true);
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
    protected void postProcessShapes(Map<String, PolygonCollection> shapes) {
        if (state.equals("Alaska")) for (PolygonCollection polygons : shapes.values()) RenderUtil.fixRollover(polygons);
    }

    public void dispose() {
        super.dispose();
        cachingHelper.dispose();
    }
}

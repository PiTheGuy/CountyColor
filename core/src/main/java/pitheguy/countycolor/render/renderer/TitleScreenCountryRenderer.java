package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.util.RenderCachingHelper;

import java.util.*;
import java.util.function.Predicate;

public class TitleScreenCountryRenderer extends RegionRenderer {
    private static final List<String> HIDDEN_STATES = List.of(
        "Guam",
        "Puerto Rico",
        "American Samoa",
        "United States Virgin Islands",
        "Commonwealth of the Northern Mariana Islands",
        "District of Columbia",
        "Alaska",
        "Hawaii"
    );
    private static final int NUM_FILLED_COUNTIES = 400;

    private final RenderCachingHelper cachingHelper;
    private final Map<String, MapColor> filledCounties = new HashMap<>();


    public TitleScreenCountryRenderer() {
        super("metadata/counties.json", properties -> !HIDDEN_STATES.contains(StateRenderer.getStateFromId(properties.getString("STATEFP"))), "STATEFP");
        cachingHelper = new RenderCachingHelper();
    }

    public void render(OrthographicCamera camera) {
        cachingHelper.render(camera, this::renderInternal);
    }

    private void renderInternal(OrthographicCamera camera) {
        ensureLoadingFinished();
        updateCamera(camera);
        renderBackground();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        filledCounties.forEach((county, color) -> fillSubregion(county, color.getColor()));
        shapeRenderer.end();
        renderRegion(camera, false, false);
    }

    @Override
    protected Map<String, PolygonCollection> loadShapes(String sourceFilePath, Predicate<JsonValue> predicate, String duplicatePreventionKey) {
        Map<String, PolygonCollection> shapes = super.loadShapes(sourceFilePath, predicate, duplicatePreventionKey);
        List<String> keys = new ArrayList<>(shapes.keySet());
        Collections.shuffle(keys);
        Random random = new Random();
        for (String key : keys.subList(0, NUM_FILLED_COUNTIES)) {
            MapColor color = MapColor.values()[random.nextInt(MapColor.values().length)];
            filledCounties.put(key, color);
        }
        return shapes;
    }

    public void invalidateCache() {
        cachingHelper.invalidateCache();
    }

    public void dispose() {
        super.dispose();
        cachingHelper.dispose();
    }
}

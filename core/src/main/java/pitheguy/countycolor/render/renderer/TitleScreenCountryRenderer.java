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

public class TitleScreenCountryRenderer implements Disposable {
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

    private final OutlineRenderer outlineRenderer;
    private final FillRenderer fillRenderer;
    private final RenderCachingHelper cachingHelper;

    public TitleScreenCountryRenderer() {
        outlineRenderer = new OutlineRenderer();
        fillRenderer = new FillRenderer();
        cachingHelper = new RenderCachingHelper();
    }

    public void render(OrthographicCamera camera) {
        cachingHelper.render(camera, this::renderInternal);
    }

    private void renderInternal(OrthographicCamera camera) {
        ensureLoadingFinished();
        fillRenderer.render(camera);
        outlineRenderer.render(camera);
    }

    private void ensureLoadingFinished() {
        outlineRenderer.ensureLoadingFinished();
        fillRenderer.ensureLoadingFinished();
    }

    public void invalidateCache() {
        cachingHelper.invalidateCache();
    }

    public void dispose() {
        outlineRenderer.dispose();
        fillRenderer.dispose();
        cachingHelper.dispose();
    }

    private static class OutlineRenderer extends RegionRenderer {
        public OutlineRenderer() {
            super("metadata/counties.json", properties -> !HIDDEN_STATES.contains(StateRenderer.getStateFromId(properties.getString("STATEFP"))), "STATEFP");
        }

        public void render(OrthographicCamera camera) {
            updateCamera(camera);
            renderRegion(camera, false, false);
        }
    }

    private static class FillRenderer extends RegionRenderer {
        Map<String, MapColor> filledCounties = new HashMap<>();
        public FillRenderer() {
            super("metadata/counties.json", properties -> !HIDDEN_STATES.contains(StateRenderer.getStateFromId(properties.getString("STATEFP"))), "STATEFP");
        }

        public void render(OrthographicCamera camera) {
            updateCamera(camera);
            renderBackground();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            filledCounties.forEach((county, color) -> fillSubregion(county, color.getColor()));
            shapeRenderer.end();
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
    }
}

package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.metadata.CountyBorders;
import pitheguy.countycolor.metadata.CountyData;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.util.RenderCachingHelper;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TitleScreenCountryRenderer extends RegionRenderer {
    private static final int NUM_FILLED_COUNTIES = 400;

    private final RenderCachingHelper cachingHelper = new RenderCachingHelper();
    private final Map<String, MapColor> filledCounties = new HashMap<>();
    private Map<String, PolygonCollection> counties;

    public void render(OrthographicCamera camera) {
        cachingHelper.render(camera, this::renderInternal);
    }

    private void renderInternal(OrthographicCamera camera) {
        ensureLoadingFinished();
        updateCamera(camera);
        renderBackground();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        filledCounties.forEach((county, color) -> fillSubregion(counties.get(county), color.getColor()));
        shapeRenderer.end();
        renderRegion(counties.values(), camera, false, false);
    }

    @Override
    protected void loadShapes() {
        counties = relativize(CountyData.getCounties(false).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getPolygons())));
        List<String> keys = new ArrayList<>(counties.keySet());
        Collections.shuffle(keys);
        Random random = new Random();
        for (String key : keys.subList(0, NUM_FILLED_COUNTIES)) {
            MapColor color = MapColor.values()[random.nextInt(MapColor.values().length)];
            filledCounties.put(key, color);
        }
    }

    public void invalidateCache() {
        cachingHelper.invalidateCache();
    }

    public void dispose() {
        super.dispose();
        cachingHelper.dispose();
    }
}

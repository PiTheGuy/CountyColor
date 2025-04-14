package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.metadata.CountyBorders;
import pitheguy.countycolor.metadata.CountyData;
import pitheguy.countycolor.render.util.RenderCachingHelper;
import pitheguy.countycolor.util.Util;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

public class CountryCompletedCountiesRenderer extends RegionRenderer {
    private final RenderCachingHelper cachingHelper = new RenderCachingHelper();
    private final BooleanSupplier usedCachedTexture;
    private Map<String, CountyData.County> counties;

    public CountryCompletedCountiesRenderer(BooleanSupplier usedCachedTexture) {
        this.usedCachedTexture = usedCachedTexture;
    }

    public void render(OrthographicCamera camera, Future<Map<String, Map<String, MapColor>>> completedCountiesFuture) {
        if (!completedCountiesFuture.isDone()) return;
        if (usedCachedTexture.getAsBoolean())
            cachingHelper.render(camera, cam -> renderInternal(cam, completedCountiesFuture, false));
        else renderInternal(camera, completedCountiesFuture, true);
    }

    private void renderInternal(OrthographicCamera camera, Future<Map<String, Map<String, MapColor>>> completedCountiesFuture, boolean cull) {
        ensureLoadingFinished();
        updateCamera(camera);
        renderBackground();
        Map<String, Map<String, MapColor>> completedCounties = Util.getFutureValue(completedCountiesFuture);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Map.Entry<String, Map<String, MapColor>> stateEntry : completedCounties.entrySet()) {
            String state = stateEntry.getKey();
            Map<String, MapColor> stateCounties = stateEntry.getValue();
            for (Map.Entry<String, MapColor> countyEntry : stateCounties.entrySet()) {
                String county = countyEntry.getKey();
                MapColor color = countyEntry.getValue();
                String key = county + " " + state;
                if (!cull || counties.get(key).getPolygons().isVisibleToCamera(camera)) fillSubregion(counties.get(key).getPolygons(), color.getColor());
            }
        }
        shapeRenderer.end();
    }

    public void invalidateCache() {
        cachingHelper.invalidateCache();
    }

    @Override
    protected void loadShapes() {
        counties = StateRenderer.rel(CountyData.getCounties(true), CountyData.getCounties(false));
    }

    @Override
    public void dispose() {
        super.dispose();
        cachingHelper.dispose();
    }
}

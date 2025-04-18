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
    private Map<String, CountyData.County> counties;

    public void render(OrthographicCamera camera, Future<Map<String, Map<String, MapColor>>> completedCountiesFuture, boolean cull) {
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

    @Override
    protected void loadShapes() {
        counties = StateRenderer.rel(CountyData.getCounties(true), CountyData.getCounties(false));
    }

}

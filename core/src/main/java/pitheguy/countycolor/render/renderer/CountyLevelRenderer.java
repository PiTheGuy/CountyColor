package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.metadata.CountyData;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.util.RenderUtil;

import java.util.*;
import java.util.function.Predicate;

import static pitheguy.countycolor.render.util.RenderConst.OUTLINE_THICKNESS;
import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public abstract class CountyLevelRenderer extends RegionRenderer {

    protected Map<String, CountyData.County> counties;
    private final List<String> independentCities = new ArrayList<>();

    protected void renderRegion(OrthographicCamera camera, boolean thick, boolean scaleThickness) {
        super.renderRegion(counties.values().stream().map(CountyData.County::getPolygons).toList(), camera, thick, scaleThickness);
        renderIndependentCities(camera, thick, scaleThickness, name -> true);
    }

    protected void renderIndependentCities(OrthographicCamera camera, boolean thick, boolean scaleThickness, Predicate<CountyData.County> predicate) {
        shapeRenderer.begin(thick ? ShapeRenderer.ShapeType.Filled : ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);
        counties.values().stream()
            .filter(CountyData.County::isIndependentCity)
            .forEach(county -> {
                if (!predicate.test(county)) return;
                if (county.getPolygons().isVisibleToCamera(camera)) return;
                if (thick) renderThickSubregionOutline(county.getPolygons(), scaleThickness ? OUTLINE_THICKNESS * camera.zoom : OUTLINE_THICKNESS);
                else renderSubregionOutline(county.getPolygons());
            });
        shapeRenderer.end();
    }

    public CountyData.County getCountyAtCoords(Vector2 coordinate) {
        return counties.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.comparing(CountyData.County::isIndependentCity).reversed()))
            .filter(entry -> {
                PolygonCollection subregion = entry.getValue().getPolygons();
                if (!subregion.boundsCheck(coordinate)) return false;
                for (List<Vector2> polygon : subregion.getPolygons())
                    if (RenderUtil.pointInPolygon(coordinate.cpy().scl(2f / RENDER_SIZE), polygon))
                        return true;
                return false;
            }).findFirst().map(Map.Entry::getValue).orElse(null);
    }

    public boolean isIndependentCity(String name) {
        return independentCities.contains(name);
    }

    @Override
    protected void postProcessJson(JsonValue json) {
        JsonValue properties = json.get("properties");
        if (Integer.parseInt(properties.getString("COUNTYFP")) > 500) independentCities.add(properties.getString("NAME"));
    }
}

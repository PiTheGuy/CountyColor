package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import static pitheguy.countycolor.render.util.RenderConst.OUTLINE_THICKNESS;
import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public abstract class CountyLevelRenderer extends RegionRenderer {
    private final List<String> independentCities = new ArrayList<>();

    public CountyLevelRenderer(Future<JsonValue> sourceJson, Predicate<JsonValue> predicate) {
        super(sourceJson, predicate);
    }

    protected void renderRegion(OrthographicCamera camera, boolean thick, boolean scaleThickness) {
        super.renderRegion(camera, thick, scaleThickness);
        renderIndependentCities(camera, thick, scaleThickness, name -> true);
    }

    protected void renderIndependentCities(OrthographicCamera camera, boolean thick, boolean scaleThickness, Predicate<String> predicate) {
        shapeRenderer.begin(thick ? ShapeRenderer.ShapeType.Filled : ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);
        for (String subregion : independentCities) {
            if (!predicate.test(subregion)) continue;
            if (!shapes.get(subregion).isVisibleToCamera(camera)) continue;
            if (thick) renderThickSubregionOutline(subregion, scaleThickness ? OUTLINE_THICKNESS * camera.zoom : OUTLINE_THICKNESS);
            else renderSubregionOutline(subregion);
        }
        shapeRenderer.end();
    }

    @Override
    public String getSubregionAtCoords(Vector2 coordinate) {
        for (String independentCity : independentCities) {
            PolygonCollection subregion = shapes.get(independentCity);
            if (!subregion.boundsCheck(coordinate)) continue;
            for (List<Vector2> polygon : subregion.getPolygons())
                if (RenderUtil.pointInPolygon(coordinate.cpy().scl(2f / RENDER_SIZE), polygon))
                    return independentCity;
        }
        return super.getSubregionAtCoords(coordinate);
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

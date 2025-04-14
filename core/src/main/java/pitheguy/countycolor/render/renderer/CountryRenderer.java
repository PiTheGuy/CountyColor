package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import pitheguy.countycolor.metadata.StateData;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.Zoom;
import pitheguy.countycolor.render.util.RenderUtil;

import java.util.*;

import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public class CountryRenderer extends RegionRenderer {
    private Map<String, PolygonCollection> states;

    public void renderCountry(OrthographicCamera camera) {
        ensureLoadingFinished();
        updateCamera(camera);
        renderRegion(states.values(), camera, false, true);
    }

    @Override
    protected void loadShapes() {
        Map<String, PolygonCollection> rawStates = new HashMap<>(StateData.getStates());
        Map<String, PolygonCollection> renderedSeparately = new HashMap<>();
        StateData.RENDERED_SEPARATELY.forEach(key -> {
            renderedSeparately.put(key, rawStates.get(key));
            rawStates.remove(key);
        });
        RenderUtil.fixRollover(renderedSeparately.get("Alaska"));
        Map<String, PolygonCollection> relativeStates = relativize(rawStates);
        addRenderedSeparately(relativeStates, renderedSeparately);
        states = relativeStates;
    }

    private void addRenderedSeparately(Map<String, PolygonCollection> map, Map<String, PolygonCollection> renderedSeparately) {
        int i = 0;
        List<Map.Entry<String, PolygonCollection>> entries = new ArrayList<>(renderedSeparately.entrySet());
        Collections.reverse(entries);
        for (Map.Entry<String, PolygonCollection> entry : entries) {
            float startX = -0.8f + 0.3f * i;
            PolygonCollection polygons = entry.getValue();
            List<List<Vector2>> relativeShapes = new ArrayList<>();
            for (List<Vector2> points : polygons.getPolygons()) {
                List<Vector2> relativePoints = new ArrayList<>();
                for (Vector2 point : points) {
                    Vector2 relativized = relativizePoint(point, polygons.getMinX(), polygons.getMaxX(), polygons.getMinY(), polygons.getMaxY());
                    relativized.scl(0.25f).add(startX, -0.4f);
                    relativePoints.add(relativized);
                }
                relativeShapes.add(relativePoints);
            }
            map.put(entry.getKey(), new PolygonCollection(relativeShapes));
            i++;
        }
    }

    public String getStateAtCoords(Vector2 coordinate) {
        for (Map.Entry<String, PolygonCollection> entry : states.entrySet()) {
            PolygonCollection subregion = entry.getValue();
            if (!subregion.boundsCheck(coordinate)) continue;
            for (List<Vector2> polygon : subregion.getPolygons())
                if (RenderUtil.pointInPolygon(coordinate.cpy().scl(2f / RENDER_SIZE), polygon))
                    return entry.getKey();
        }
        return null;
    }

    public Zoom getTargetZoom(String state) {
        return getTargetZoom(states.get(state));
    }
}

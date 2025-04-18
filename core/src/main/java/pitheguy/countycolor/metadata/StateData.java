package pitheguy.countycolor.metadata;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.renderer.RegionRenderer;

import java.util.*;

public class StateData {
    public static final List<String> FILTERED_STATES = List.of(
        "Guam",
        "Puerto Rico",
        "American Samoa",
        "United States Virgin Islands",
        "Commonwealth of the Northern Mariana Islands",
        "District of Columbia"
    );
    public static final List<String> RENDERED_SEPARATELY = List.of("Alaska", "Hawaii");
    private static Map<String, PolygonCollection> states;

    public static Map<String, PolygonCollection> getStates() {
        if (states == null) load();
        return states;
    }

    public static void load() {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(Gdx.files.internal("metadata/states.json"));
        JsonValue array = root.get("features");
        Map<String, PolygonCollection> states = new HashMap<>();
        for (JsonValue stateJson : array) {
            JsonValue properties = stateJson.get("properties");
            PolygonCollection polygons = RegionRenderer.loadSubregion(stateJson);
            states.put(properties.getString("NAME"), polygons);
        }
        FILTERED_STATES.forEach(states::remove);
        StateData.states = states;
    }
}

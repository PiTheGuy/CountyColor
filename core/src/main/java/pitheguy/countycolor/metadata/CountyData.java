package pitheguy.countycolor.metadata;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.renderer.RegionRenderer;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CountyData {

    private static Map<String, Map<String, County>> counties = null;

    public static void load() {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(Gdx.files.internal("metadata/counties.json"));
        JsonValue array = root.get("features");
        Map<String, Map<String, County>> counties = new HashMap<>();
        for (JsonValue countyJson : array) {
            JsonValue properties = countyJson.get("properties");
            PolygonCollection polygons = RegionRenderer.loadSubregion(countyJson);
            County county = new County(polygons, properties);
            Map<String, County> stateMap = counties.computeIfAbsent(county.getState(), s -> new HashMap<>());
            stateMap.put(county.getName(), county);
        }
        CountyData.counties = Collections.unmodifiableMap(counties);
    }

    public static Map<String, County> getCounties(boolean includeRenderedSeparately) {
        if (counties == null) load();
        return counties.entrySet().stream()
            .filter(e -> !StateData.FILTERED_STATES.contains(e.getKey()) && (includeRenderedSeparately || !StateData.RENDERED_SEPARATELY.contains(e.getKey())))
            .flatMap(entry -> entry.getValue().values().stream())
            .collect(Collectors.toMap(county -> county.getName() + " " + county.getState(), Function.identity()));
    }

    public static Map<String, County> getCountiesForState(String state) {
        if (counties == null) load();
        return counties.get(state);
    }

    public static County getCounty(String county, String state) {
        if (counties == null) load();
        return counties.get(state).get(county);
    }

    public static class County {
        private final PolygonCollection polygons;
        private final String name;
        private final String fullName;
        private final String state;
        private final String id;

        public County(PolygonCollection polygons, String name, String fullName, String state, String id) {
            this.polygons = polygons;
            this.name = name;
            this.fullName = fullName;
            this.state = state;
            this.id = id;
        }

        public County(PolygonCollection polygons, JsonValue properties) {
            this(polygons, properties.getString("NAME"), properties.getString("NAMELSAD"), properties.getString("STATE_NAME"), properties.getString("COUNTYFP"));
        }

        public PolygonCollection getPolygons() {
            return polygons;
        }

        public String getName() {
            return name;
        }

        public String getFullName() {
            return fullName;
        }

        public String getState() {
            return state;
        }

        public String getId() {
            return id;
        }

        public boolean isIndependentCity() {
            return Integer.parseInt(id) > 500;
        }

        public County withPolygons(PolygonCollection polygons) {
            return new County(polygons, name, fullName, state, id);
        }
    }
}

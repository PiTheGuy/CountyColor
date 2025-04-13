package pitheguy.countycolor.metadata;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class CountyBorders {
    private static JsonValue json;

    public static JsonValue getJson() {
        if (json == null) {
            JsonReader reader = new JsonReader();
            json = reader.parse(Gdx.files.internal("metadata/counties.json"));
        }
        return json;
    }
}

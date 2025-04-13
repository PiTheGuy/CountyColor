package pitheguy.countycolor.metadata;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.concurrent.*;

public class CountyBorders {
    private static JsonValue json;

    public static Future<JsonValue> getJson() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<JsonValue> result = executor.submit(() -> {
            if (json == null) {
                JsonReader reader = new JsonReader();
                json = reader.parse(Gdx.files.internal("metadata/counties.json"));
            }
            return json;
        });
        executor.shutdown();
        return result;
    }
}

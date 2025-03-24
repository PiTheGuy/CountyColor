package pitheguy.countycolor.util;

import com.google.gson.JsonObject;

public class GsonUtil {
    public static JsonObject getOrCreateChild(JsonObject parent, String childName) {
        JsonObject child = parent.getAsJsonObject(childName);
        if (child == null) {
            child = new JsonObject();
            parent.add(childName, child);
        }
        return child;
    }
}

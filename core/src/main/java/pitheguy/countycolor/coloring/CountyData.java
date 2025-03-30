package pitheguy.countycolor.coloring;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.*;
import java.util.concurrent.*;

public class CountyData {
    public static final CountyData EMPTY = new CountyData(Map.of());
    private final Map<String, Entry> entries;

    public CountyData(Map<String, Entry> entries) {
        this.entries = entries;
    }

    public Entry get(String county) {
        return entries.getOrDefault(county, Entry.EMPTY);
    }

    public static Future<CountyData> loadAsync(String state) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> {
            FileHandle dataHandle = Gdx.files.local("data/" + state + ".json");
            if (!dataHandle.exists()) return CountyData.EMPTY;

            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(dataHandle);
            Map<String, Entry> entries = new HashMap<>();
            for (JsonValue county = root.child; county != null; county = county.next) {
                String countyName = county.name;
                MapColor color = MapColor.fromSerializedName(county.getString("color"));
                float completion = county.getFloat("completion");
                entries.put(countyName, new Entry(color, completion));
            }
            return new CountyData(entries);
        });
    }

    public static final class Entry {
        public static final Entry EMPTY = new Entry(null, 0);
        private final MapColor mapColor;
        private final float completion;

        public Entry(MapColor mapColor, float completion) {
            this.mapColor = mapColor;
            this.completion = completion;
        }

        public boolean isCompleted() {
            return completion == 1;
        }

        public boolean isStarted() {
            return completion > 0;
        }

        public String getCompletionString() {
            if (completion == 0) return "Not Started";
            if (completion == 1) return "Completed";
            return String.format("%.2f%% Complete", completion * 100);
        }

        public MapColor mapColor() {
            return mapColor;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            Entry that = (Entry) obj;
            return this.mapColor == that.mapColor && this.completion == that.completion;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mapColor, completion);
        }

        @Override
        public String toString() {
            return "Entry[" +
                   "mapColor=" + mapColor + ", " +
                   "completion=" + completion + ']';
        }

    }
}

package pitheguy.countycolor.options;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.*;

import static pitheguy.countycolor.options.OptionsRegistry.register;

public class Options {
    public static final Option<Boolean> REDUCE_MOTION = register(new Option<>("reduce_motion", OptionType.BOOLEAN, false));
    public static final Option<Boolean> ASYNC_GRID_UPDATES = register(new Option<>("async_grid_updates", OptionType.BOOLEAN, true));
    public static final Option<Boolean> ENFORCE_MAP_COLORS = register(new Option<>("enforce_map_colors", OptionType.BOOLEAN, true));

    public static void save() {
        JsonValue json = new JsonValue(JsonValue.ValueType.object);
        for (Option<?> option : OptionsRegistry.getOptions()) option.serialize(json);
        Gdx.files.local("options.json").writeString(json.toJson(JsonWriter.OutputType.json), false);
    }

    public static void load() {
        FileHandle handle = Gdx.files.local("options.json");
        if (!handle.exists()) return;
        JsonReader reader = new JsonReader();
        JsonValue json = reader.parse(handle);
        for (Option<?> option : OptionsRegistry.getOptions()) option.deserialize(json);
    }
}

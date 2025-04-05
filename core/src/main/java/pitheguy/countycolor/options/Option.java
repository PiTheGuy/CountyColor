package pitheguy.countycolor.options;

import com.badlogic.gdx.utils.JsonValue;

public class Option<T> {
    private final String key;
    private final OptionType<T> type;
    private final T defaultValue;
    private T value;

    public Option(String key, OptionType<T> type, T defaultValue) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
        this.value = this.defaultValue;
    }

    public String getKey() {
        return key;
    }

    public void serialize(JsonValue json) {
        json.addChild(key, type.serialize(value));
    }

    public void deserialize(JsonValue json) {
        JsonValue settingJson = json.get(key);
        if (settingJson != null) value = type.deserialize(settingJson);
        else value = defaultValue;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}

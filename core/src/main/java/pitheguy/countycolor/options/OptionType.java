package pitheguy.countycolor.options;

import com.badlogic.gdx.utils.JsonValue;

import java.util.function.Function;

public class OptionType<T> {
    public static OptionType<Boolean> BOOLEAN = new OptionType<>(Boolean.class, JsonValue::new, JsonValue::asBoolean);

    private final Class<T> type;
    private final Function<T, JsonValue> serializer;
    private final Function<JsonValue, T> deserializer;

    public OptionType(Class<T> type, Function<T, JsonValue> serializer, Function<JsonValue, T> deserializer) {
        this.type = type;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    public Class<T> getType() {
        return type;
    }

    public JsonValue serialize(T value) {
        return serializer.apply(value);
    }

    public T deserialize(JsonValue json) {
        return deserializer.apply(json);
    }
}

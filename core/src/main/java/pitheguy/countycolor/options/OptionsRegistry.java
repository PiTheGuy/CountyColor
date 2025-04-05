package pitheguy.countycolor.options;

import java.util.*;

public class OptionsRegistry {
    private static final Set<String> usedKeys = new HashSet<>();
    private static final List<Option<?>> options = new ArrayList<>();

    public static <T> Option<T> register(Option<T> option) {
        if (!usedKeys.add(option.getKey())) throw new IllegalArgumentException("Duplicate option key: " + option.getKey());
        options.add(option);
        return option;
    }

    public static List<Option<?>> getOptions() {
        return options;
    }
}

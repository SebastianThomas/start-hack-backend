package ch.sthomas.hack.start.model.util;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class MapCollectors {

    private MapCollectors() {}

    @NotNull
    public static <K, V, T extends Map.Entry<? extends K, ? extends V>>
            Collector<T, ?, Map<K, V>> entriesToMap() {
        return Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    @NotNull
    public static <K, V, T extends Map.Entry<? extends K, ? extends V>>
            Collector<T, ?, Map<K, V>> entriesToMap(
                    @NotNull final BinaryOperator<V> mergeFunction) {
        return Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, mergeFunction);
    }
}

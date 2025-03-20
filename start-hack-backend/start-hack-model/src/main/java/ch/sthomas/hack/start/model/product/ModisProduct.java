package ch.sthomas.hack.start.model.product;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public enum ModisProduct {
    LCT(
            f -> !List.of(0, 128).contains((int) f.getProperty("DN")),
            f -> Map.ofEntries(Map.entry("landUse", getLandUseFromMinElev(f.getProperty("DN"))))),
    GP(f -> true, f -> Map.of("prop", f.getProperty("DN"))); // TODO

    private final Predicate<BaseFeature> include;
    private final Function<BaseFeature, Map<String, Object>> properties;

    ModisProduct(
            final Predicate<BaseFeature> include,
            final Function<BaseFeature, Map<String, Object>> properties) {
        this.include = include;
        this.properties = properties;
    }

    public Predicate<BaseFeature> include() {
        return include;
    }

    public Map<String, Object> properties(final BaseFeature feature) {
        return properties.apply(feature);
    }

    public List<BaseFeature> mapAndFilter(final BaseFeatureCollection features) {
        return features.getFeatures().stream()
                .filter(include)
                .map(
                        f ->
                                new BaseFeature()
                                        .setId(f.getId())
                                        .setGeometry(f.getGeometry())
                                        .setType(f.getType())
                                        .setProperties(properties(f)))
                .toList();
    }

    private static String getLandUseFromMinElev(final int minElev) {
        return switch (minElev) {
            case 7 -> "Open Shrublands";
            case 10 -> "Grasslands";
            case 12 -> "Croplands";
            case 13 -> "Urban and Built-up Lands";
            case 16 -> "Barren";
            default -> throw new IllegalArgumentException("Unknown id: " + minElev);
        };
    }
}

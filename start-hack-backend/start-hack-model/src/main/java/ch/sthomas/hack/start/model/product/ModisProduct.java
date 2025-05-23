package ch.sthomas.hack.start.model.product;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public enum ModisProduct {
    LCT(
            true,
            f -> !List.of(0, 128).contains((int) f.getProperty("DN")),
            f -> Map.ofEntries(Map.entry("landUse", getLandUseFromKey(f.getProperty("DN")))),
            ob -> (int) ((byte[]) ob)[0]),
    GP(true, f -> true, f -> Map.of("gp", f.getProperty("DN")), ob -> ((int[]) ob)[0]),
    GP_SIMPLIFIED(
            true,
            f -> true,
            f -> Map.of("gp-simplified", f.getProperty("DN")),
            ob -> ((int[]) ob)[0]),
    POPULATION_DENSITY(
            true, f -> true, f -> Map.of("pop_dens", f.getProperty("DN")), Function.identity()),
    CLIMATE_PRECIPITATION(
            true,
            f -> true,
            f -> Map.of("climate_precip", f.getProperty("DN")),
            Function.identity()),
    ;

    private final boolean invert;
    private final Predicate<BaseFeature> include;
    private final Function<BaseFeature, Map<String, Object>> properties;
    private final Function<Object, ?> mapPointEval;

    <T> ModisProduct(
            final boolean invert,
            final Predicate<BaseFeature> include,
            final Function<BaseFeature, Map<String, Object>> properties,
            final Function<Object, T> mapPointEval) {
        this.invert = invert;
        this.include = include;
        this.properties = properties;
        this.mapPointEval = mapPointEval;
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

    public static String getLandUseFromKey(final int minElev) {
        return switch (minElev) {
            case 7 -> "Open Shrublands";
            case 10 -> "Grasslands";
            case 12 -> "Croplands";
            case 13 -> "Urban and Built-up Lands";
            case 16 -> "Barren";
            default -> throw new IllegalArgumentException("Unknown id: " + minElev);
        };
    }

    public boolean invert() {
        return invert;
    }

    public boolean simplify() {
        return this == GP_SIMPLIFIED;
    }

    public <T> Function<Object, T> mapPointEval() {
        return (Function<Object, T>) mapPointEval;
    }
}

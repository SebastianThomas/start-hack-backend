package ch.sthomas.hack.start.model.geo;

import com.pivovarit.function.ThrowingSupplier;

import jakarta.validation.constraints.NotNull;

import org.geotools.api.data.SimpleFeatureReader;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GeotoolsUtils {

    private GeotoolsUtils() {}

    @NotNull
    public static Stream<SimpleFeature> stream(final SimpleFeatureReader featureReader) {
        return generateAsLongAsPresent(nextOptional(featureReader).uncheck());
    }

    @NotNull
    public static Stream<SimpleFeature> stream(final SimpleFeatureIterator featureIterator) {
        return generateAsLongAsPresent(nextOptional(featureIterator));
    }

    @NotNull
    public static ThrowingSupplier<Optional<SimpleFeature>, IOException> nextOptional(
            final SimpleFeatureReader reader) {
        return () -> reader.hasNext() ? Optional.of(reader.next()) : Optional.empty();
    }

    @NotNull
    public static Supplier<Optional<SimpleFeature>> nextOptional(
            final SimpleFeatureIterator iterator) {
        return () -> iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }

    @NotNull
    public static Map<String, Object> getProperties(final SimpleFeature feature) {
        return feature.getProperties().stream()
                .filter(f -> f.getValue() != null)
                .collect(Collectors.toMap(f -> f.getName().toString(), Property::getValue));
    }

    @NotNull
    private static <T> Stream<T> generateAsLongAsPresent(
            final Supplier<Optional<T>> optionalSupplier) {

        return Stream.generate(optionalSupplier)
                .takeWhile(Optional::isPresent)
                .map(Optional::orElseThrow);
    }
}

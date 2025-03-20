package ch.sthomas.hack.start.service;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

@Service
public class OSMDataService {
    private final Path data;
    private final Path outputFolder;
    private final ObjectMapper objectMapper;

    public OSMDataService(
            @Value("${ch.sthomas.hack.start.service.data.folder}") final String dataFolder,
            @Value("${ch.sthomas.hack.start.public.folder}") final String outputFolder,
            final ObjectMapper objectMapper) {
        this.data = Path.of(dataFolder);
        this.outputFolder = Path.of(outputFolder);
        this.objectMapper = objectMapper;
    }

    public void loadAndSaveData() throws IOException {
        final var points = loadFeatureCollection(data.resolve("mauritania-points.geojson"));
        final var allPoints =
                filterFeatureCollection(
                        points,
                        f -> f.getProperties().values().stream().anyMatch(Objects::nonNull));
        final var manMadePoints = filterContainingKey(allPoints, "man_made");
        objectMapper.writeValue(
                outputFolder.resolve("assaba-all-points.geojson").toFile(), allPoints);
        objectMapper.writeValue(
                outputFolder.resolve("assaba-man-made-points.geojson").toFile(), manMadePoints);

        final var lines = loadFeatureCollection(data.resolve("mauritania-lines.geojson"));
        final var highways = filterContainingKey(lines, "highway");
        final var waterways =
                filterFeatureCollection(
                        lines,
                        f ->
                                f.getProperties().containsKey("waterway")
                                        || f.getProperties()
                                                .getOrDefault("natural", "no")
                                                .equals("water"));
        objectMapper.writeValue(outputFolder.resolve("assaba-lines.geojson").toFile(), lines);
        objectMapper.writeValue(outputFolder.resolve("assaba-highways.geojson").toFile(), highways);
        objectMapper.writeValue(
                outputFolder.resolve("assaba-waterways.geojson").toFile(), waterways);
    }

    private BaseFeatureCollection filterContainingKey(
            final BaseFeatureCollection featureCollection, final String key) {
        return filterFeatureCollection(featureCollection, f -> f.getProperties().containsKey(key));
    }

    private BaseFeatureCollection filterFeatureCollection(
            final BaseFeatureCollection featureCollection, final Predicate<BaseFeature> predicate) {
        return new BaseFeatureCollection()
                .setFeatures(featureCollection.getFeatures().stream().filter(predicate).toList());
    }

    private BaseFeatureCollection loadFeatureCollection(final Path path) throws IOException {
        return objectMapper.readValue(path.toFile(), BaseFeatureCollection.class);
    }
}

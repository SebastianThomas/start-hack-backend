package ch.sthomas.hack.start.service;

import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

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
        final var points =
                new BaseFeatureCollection()
                        .setFeatures(
                                objectMapper
                                        .readValue(
                                                data.resolve("mauritania-points.geojson").toFile(),
                                                BaseFeatureCollection.class)
                                        .getFeatures()
                                        .stream()
                                        .filter(
                                                f ->
                                                        f.getProperties().values().stream()
                                                                .anyMatch(Objects::nonNull))
                                        .toList());
        final var lines =
                new BaseFeatureCollection()
                        .setFeatures(
                                objectMapper
                                        .readValue(
                                                data.resolve("mauritania-lines.geojson").toFile(),
                                                BaseFeatureCollection.class)
                                        .getFeatures()
                                        .stream()
                                        .filter(f -> f.getProperty("highway") != null)
                                        .toList());
        objectMapper.writeValue(outputFolder.resolve("assaba-points.geojson").toFile(), points);
        objectMapper.writeValue(outputFolder.resolve("assaba-lines.geojson").toFile(), lines);
    }
}

package ch.sthomas.hack.start.service.shapefile;

import ch.sthomas.hack.start.model.geo.GeotoolsUtils;

import jakarta.validation.constraints.NotNull;

import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class ShapefileParser {

    private static final Logger logger = LoggerFactory.getLogger(ShapefileParser.class);

    private final Path path;
    private final String datasetName;

    public ShapefileParser(@NotNull final Path path, @NotNull final String datasetName) {
        this.path = Objects.requireNonNull(path);
        this.datasetName = Objects.requireNonNull(datasetName);
    }

    @NotNull
    public Collection<SimpleFeature> loadData() throws IOException {
        final var files = findFiles();
        return parseFeatures(files);
    }

    private Collection<Path> findFiles() {
        logger.info("Parsing shape files from {}", path.toAbsolutePath());
        final var arr = path.toFile().listFiles((file, name) -> name.contains(datasetName));
        if (arr == null) {
            return List.of();
        }
        return Arrays.stream(arr).map(File::toPath).toList();
    }

    private Collection<SimpleFeature> parseFeatures(final Collection<Path> files)
            throws IOException {

        final var shapeFile =
                files.stream()
                        .filter(f -> f.getFileName().toString().endsWith(".shp"))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException("No .shp file found."));

        final var dataStore =
                (ShapefileDataStore)
                        DataStoreFinder.getDataStore(
                                Map.of("url", shapeFile.toFile().toURI().toURL()));
        dataStore.setCharset(StandardCharsets.UTF_8);

        final var typeName = dataStore.getTypeNames()[0];

        final var source = dataStore.getFeatureSource(typeName);
        final var filter = Filter.INCLUDE;

        final var featureIterator = source.getFeatures(filter).features();

        final var features = GeotoolsUtils.stream(featureIterator).toList();

        logger.info("Parsed {} features", features.size());

        featureIterator.close();
        dataStore.dispose();

        logger.info("Deleted temporary directory");

        return features;
    }
}

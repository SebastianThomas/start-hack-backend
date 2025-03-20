package ch.sthomas.hack.start.service.geo;

import static java.util.Objects.requireNonNull;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;
import ch.sthomas.hack.start.model.product.ModisProduct;
import ch.sthomas.hack.start.service.tif.TifParser;
import ch.sthomas.hack.start.service.utils.ProcessUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.image.Raster;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
public class GridCoverageService {

    private static final Logger logger = LoggerFactory.getLogger(GridCoverageService.class);
    private static final GridCoverageFactory GRID_COVERAGE_FACTORY = new GridCoverageFactory();
    private final ObjectMapper objectMapper;

    GridCoverageService(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Nullable
    public GridCoverage2D warpToWGS84(final GridCoverage2D gridCoverage) {
        if (gridCoverage == null) {
            return null;
        }

        try {
            final var sourceFile = write(gridCoverage);
            final var transformed = warpToWGS84(sourceFile);
            Files.delete(sourceFile);
            return transformed;
        } catch (final IOException ioException) {
            logger.warn("Could not transform grid coverage", ioException);
            return gridCoverage;
        }
    }

    @Nullable
    private GridCoverage2D warpToWGS84(final @NotNull Path geoReferencedFile) throws IOException {
        requireNonNull(geoReferencedFile);

        final var warpedFile = createTempFile("warped-");
        final var command =
                List.of(
                        "gdalwarp",
                        "-t_srs",
                        "+proj=longlat +datum=WGS84 +no_defs +axis=enu",
                        "-overwrite",
                        geoReferencedFile.toString(),
                        warpedFile.toString());
        final var result = ProcessUtils.executeProcess(new ProcessBuilder(command));
        if (result
                instanceof final ProcessUtils.ProcessDidNotFinishResult processDidNotFinishResult) {
            logger.warn("Process didn't finish result: {}", processDidNotFinishResult.stderr());
            return null;
        }

        final var warped = read(warpedFile);
        Files.delete(warpedFile);
        return warped;
    }

    public BaseFeatureCollection vectorize(final GridCoverage2D gridCoverage) {
        return vectorize(gridCoverage, false);
    }

    public BaseFeatureCollection vectorize(
            final GridCoverage2D gridCoverage, final boolean simplify) {
        try {
            final var contours = polygons(gridCoverage);
            if (simplify) {
                return simplify(contours);
            }
            return contours;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public BaseFeatureCollection polygons(final GridCoverage2D gridCoverage) throws IOException {
        final var src = write(gridCoverage);
        // Don't create the temp file directly - gdal cannot override, only create a new file.
        final var result = Files.createTempDirectory("contours").resolve("contours.geojson");
        final var gdal =
                ProcessUtils.executeProcess(
                        new ProcessBuilder(
                                List.of(
                                        // In docker container, .py is required
                                        "gdal_polygonize.py",
                                        src.toAbsolutePath().toString(), // src
                                        result.toString(), // dest
                                        // "-fl", // Levels with params
                                        // "7 10 12 13 16",
                                        // "-p", // Polygons
                                        // "-amin",
                                        // "MIN_ELEV",
                                        "DN",
                                        "-q" // quiet
                                        )));
        if (gdal instanceof ProcessUtils.ProcessFinishedResult) {
            final var gdalResult =
                    objectMapper.readValue(result.toFile(), BaseFeatureCollection.class);
            return new BaseFeatureCollection()
                    .setFeatures(
                            gdalResult.getFeatures().stream()
                                    .map(GridCoverageService::gdalToFeature)
                                    .toList());
        }
        throw new IOException("Could not contour file.");
    }

    private static BaseFeature gdalToFeature(final BaseFeature f) {
        return new BaseFeature()
                .setId(UUID.randomUUID().toString())
                .setProperties(f.getProperties())
                .setType(f.getType())
                .setGeometry(f.getGeometry());
    }

    public static BaseFeatureCollection simplify(final BaseFeatureCollection featureCollection) {
        return new BaseFeatureCollection()
                .setFeatures(
                        featureCollection.getFeatures().stream()
                                .map(
                                        f ->
                                                new BaseFeature()
                                                        .setId(f.getId())
                                                        .setProperties(f.getProperties())
                                                        .setType(f.getType())
                                                        .setGeometry(
                                                                simplifyGeometry(f.getGeometry())))
                                .toList());
    }

    private static Geometry simplifyGeometry(final Geometry geometry) {
        return DouglasPeuckerSimplifier.simplify(geometry, 0.01);
    }

    public static Path write(final @NotNull GridCoverage gridCoverage) throws IOException {
        final var inputGribFile = createTempFile("grid-coverage");
        final var writer = new GeoTiffWriter(inputGribFile.toFile());
        writer.write(gridCoverage, null);
        logger.trace("Wrote grid coverage to {}.", inputGribFile);
        return inputGribFile;
    }

    public static GridCoverage2D read(final @NotNull Path path) throws IOException {
        return new TifParser(path).parse();
    }

    public static float[] getValuesArray(final @NotNull GridCoverage gridCoverage) {
        final var raster = getRaster(gridCoverage);
        return getValuesArray(raster);
    }

    public static Raster getRaster(final @NotNull GridCoverage gridCoverage) {
        return gridCoverage.getRenderedImage().getData();
    }

    public static float[] getValuesArray(final @NotNull Raster raster) {
        return raster.getPixels(
                raster.getMinX(),
                raster.getMinY(),
                raster.getWidth(),
                raster.getHeight(),
                (float[]) null);
    }

    public static @NotNull GridCoverage2D withEnvelope(
            final @NotNull GridCoverage2D gridCoverage,
            final @NotNull ReferencedEnvelope envelope) {
        return GRID_COVERAGE_FACTORY.create(
                gridCoverage.getName(),
                gridCoverage.getRenderedImage(),
                envelope,
                null,
                null,
                gridCoverage.getProperties());
    }

    private static Path createTempFile(final String prefix) throws IOException {
        return Files.createTempFile(prefix + "_", ".tif");
    }

    public Function<GridCoverage2D, GridCoverage2D> simplifyGrid(final ModisProduct product) {
        if (!product.simplify()) {
            return f -> f;
        }

        return grid -> {
            final var originalImage = grid.getRenderedImage();
            final var raster = originalImage.copyData(null);

            final var width = raster.getWidth();
            final var height = raster.getHeight();

            // Loop through each pixel and modify its value based on the mapping
            for (var y = 0; y < height; y++) {
                for (var x = 0; x < width; x++) {
                    final var originalValue =
                            raster.getSample(x, y, 0); // Assuming single-band raster
                    final var newValue = simplifyValue(originalValue); // Apply mapping function
                    raster.setSample(x, y, 0, newValue);
                }
            }

            // Create a new GridCoverage2D from the modified raster
            final var factory = new GridCoverageFactory();
            final var envelope = grid.getEnvelope2D();

            return factory.create("SimplifiedCoverage", raster, envelope);
        };
    }

    private int simplifyValue(final int value) {
        if (value <= 0) return value; // Edge case

        final var magnitude =
                (int) Math.pow(10, Math.floor(Math.log10(value))); // Find nearest lower power of 10
        final var firstDigit = value / magnitude; // Extract first digit

        // Round first digit to 1, 2, or 5
        if (firstDigit <= 2) return magnitude;
        if (firstDigit <= 5) return 2 * magnitude;
        return 5 * magnitude;
    }
}

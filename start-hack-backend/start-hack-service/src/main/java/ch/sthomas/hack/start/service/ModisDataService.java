package ch.sthomas.hack.start.service;

import static ch.sthomas.hack.start.model.product.ModisProduct.*;

import static java.time.ZoneOffset.UTC;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;
import ch.sthomas.hack.start.model.points.PointData;
import ch.sthomas.hack.start.model.product.ModisProduct;
import ch.sthomas.hack.start.model.util.MapCollectors;
import ch.sthomas.hack.start.service.geo.GridCoverageService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.geotools.api.coverage.PointOutsideCoverageException;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class ModisDataService {
    private static final Logger logger = LoggerFactory.getLogger(ModisDataService.class);
    private final Path modisLctFolder;
    private final Path modisGPFolder;
    private final Path populationDensityFolder;
    private final Path climatePrecipitationFolder;
    private final Path outputFolder;
    private final GeoService geoService;
    private final GridCoverageService gridCoverageService;
    private final ObjectMapper objectMapper;
    private final CoordinateReferenceSystem wgs84;

    public ModisDataService(
            @Value("${ch.sthomas.hack.start.service.modis-lct.folder}") final String modisLctFolder,
            @Value("${ch.sthomas.hack.start.service.modis-gp.folder}") final String modisGPFolder,
            @Value("${ch.sthomas.hack.start.service.population-density.folder}")
                    final String populationDensityFolder,
            @Value("${ch.sthomas.hack.start.service.climate-precipitation.folder}")
                    final String climatePrecipitationFolder,
            @Value("${ch.sthomas.hack.start.public.folder}") final String outputFolder,
            final GeoService geoService,
            final GridCoverageService gridCoverageService,
            final ObjectMapper objectMapper)
            throws FactoryException {
        this.geoService = geoService;
        this.outputFolder = Paths.get(outputFolder);
        this.modisLctFolder = Path.of(modisLctFolder);
        this.modisGPFolder = Path.of(modisGPFolder);
        this.populationDensityFolder = Path.of(populationDensityFolder);
        this.climatePrecipitationFolder = Path.of(climatePrecipitationFolder);
        this.gridCoverageService = gridCoverageService;
        this.objectMapper = objectMapper;
        wgs84 = CRS.decode("EPSG:4326");
    }

    public void loadAndSaveData() {
        for (final var value : values()) {
            loadAndSaveData(value);
        }
    }

    public void loadAndSaveData(final ModisProduct product) {
        final var path =
                switch (product) {
                    case LCT -> modisLctFolder;
                    case GP, GP_SIMPLIFIED -> modisGPFolder;
                    case POPULATION_DENSITY -> populationDensityFolder;
                    case CLIMATE_PRECIPITATION -> climatePrecipitationFolder;
                };
        final IntFunction<String> productPathFilename =
                year ->
                        switch (product) {
                            case LCT -> year + "LCT.tif";
                            case GP, GP_SIMPLIFIED -> year + "_GP.tif";
                            case POPULATION_DENSITY -> "Assaba_Pop_" + year + ".tif";
                            case CLIMATE_PRECIPITATION -> year + "R.tif";
                        };
        final var gdalToFeature = gdalToFeature(product);
        final var years =
                IntStream.range(2000, 2024)
                        .<Optional<Map.Entry<Integer, BaseFeatureCollection>>>mapToObj(
                                year -> {
                                    try {
                                        final var filename = productPathFilename.apply(year);
                                        final var productPath = path.resolve(filename);
                                        return Optional.ofNullable(geoService.getTif(productPath))
                                                .map(gridCoverageService.simplifyGrid(product))
                                                .map(gridCoverageService::vectorize)
                                                .map(gdalToFeature)
                                                .map(f -> product.invert() ? invertCoords(f) : f)
                                                .map(features -> Map.entry(year, features));
                                    } catch (final IOException e) {
                                        logger.info("Could not read Tif file and contour", e);
                                        return Optional.empty();
                                    }
                                })
                        .flatMap(Optional::stream)
                        .collect(MapCollectors.entriesToMap());
        years.forEach(
                (year, vectors) -> {
                    final var outputFileName =
                            product.name().toLowerCase() + "-" + year + ".geojson";
                    final var outputFile = outputFolder.resolve(outputFileName).toFile();
                    outputFile.getParentFile().mkdirs();
                    try {
                        objectMapper.writeValue(outputFile, vectors);
                        logger.debug("Saved vectors to {}", outputFile.toPath().toAbsolutePath());
                    } catch (final IOException e) {
                        logger.info(e.getMessage());
                    }
                });
    }

    private Stream<PointData<Object>> getPointData(
            final ModisProduct product, final Position point) {
        final var path =
                switch (product) {
                    case LCT -> modisLctFolder;
                    case GP, GP_SIMPLIFIED -> modisGPFolder;
                    case POPULATION_DENSITY -> populationDensityFolder;
                    case CLIMATE_PRECIPITATION -> climatePrecipitationFolder;
                };
        final IntFunction<String> productPathFilename =
                year ->
                        switch (product) {
                            case LCT -> year + "LCT.tif";
                            case GP, GP_SIMPLIFIED -> year + "_GP.tif";
                            case POPULATION_DENSITY -> "Assaba_Pop_" + year + ".tif";
                            case CLIMATE_PRECIPITATION -> year + "R.tif";
                        };
        return IntStream.range(2000, 2024)
                .<Optional<PointData<Object>>>mapToObj(
                        year -> {
                            try {
                                final var filename = productPathFilename.apply(year);
                                final var productPath = path.resolve(filename);
                                return Optional.ofNullable(geoService.getTif(productPath))
                                        .map(
                                                g -> {
                                                    try {
                                                        if (product.invert()) {
                                                            final var tmp = point.getOrdinate(0);
                                                            point.setOrdinate(
                                                                    0, point.getOrdinate(1));
                                                            point.setOrdinate(1, tmp);
                                                        }
                                                        return g.evaluate(point);
                                                    } catch (
                                                            final PointOutsideCoverageException e) {
                                                        logger.warn(
                                                                "Could not load point {}",
                                                                point,
                                                                e);
                                                        return Optional.empty();
                                                    }
                                                })
                                        .map(
                                                d ->
                                                        new PointData<>(
                                                                instantFromYear(year), product, d));
                            } catch (IOException e) {
                                return Optional.empty();
                            }
                        })
                .flatMap(Optional::stream);
    }

    private Instant instantFromYear(final int year) {
        return Instant.from(ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, UTC));
    }

    private UnaryOperator<BaseFeatureCollection> gdalToFeature(final ModisProduct product) {
        return features -> new BaseFeatureCollection().setFeatures(product.mapAndFilter(features));
    }

    private BaseFeatureCollection invertCoords(final BaseFeatureCollection featureCollection) {
        return new BaseFeatureCollection()
                .setFeatures(
                        featureCollection.getFeatures().stream()
                                .map(this::invertCoordsAndFilterSmall)
                                .filter(Objects::nonNull)
                                .toList());
    }

    private BaseFeature invertCoordsAndFilterSmall(final BaseFeature flipped) {
        final var inverted = invertCoordsAndFilterSmall(flipped.getGeometry());
        if (inverted == null) {
            return null;
        }
        return new BaseFeature()
                .setId(flipped.getId())
                .setProperties(flipped.getProperties())
                .setType(flipped.getType())
                .setGeometry(inverted);
    }

    private Geometry invertCoordsAndFilterSmall(final Geometry geometry) {
        return switch (geometry) {
            case final MultiPolygon p ->
                    new MultiPolygon(
                            IntStream.range(0, p.getNumGeometries())
                                    .mapToObj(i -> (Polygon) p.getGeometryN(i))
                                    .map(this::invertPolygonCoords)
                                    .filter(Objects::nonNull)
                                    .toArray(Polygon[]::new),
                            p.getFactory());
            case final Polygon p -> invertPolygonCoords(p);
            default -> geometry;
        };
    }

    // Helper method to invert the coordinates of a Polygon
    private Polygon invertPolygonCoords(final Polygon polygon) {
        // Invert the coordinates of the outer ring and any inner rings (holes).
        final var exterior = polygon.getExteriorRing();
        if (polygon.getNumInteriorRing() == 1
                && exterior.distance(polygon.getInteriorRingN(0)) < 0.004) {
            return null;
        }

        final var invertedExterior = invertCoordinatesOfRing(exterior);

        final var invertedHoles = new ArrayList<LinearRing>();
        for (var i = 0; i < polygon.getNumInteriorRing(); i++) {
            final var hole = polygon.getInteriorRingN(i);
            invertedHoles.add(invertCoordinatesOfRing(hole));
        }

        // Create a new polygon with the inverted coordinates.
        return new Polygon(
                invertedExterior, invertedHoles.toArray(new LinearRing[0]), polygon.getFactory());
    }

    // Helper method to invert the coordinates of a LinearRing
    private LinearRing invertCoordinatesOfRing(final LinearRing ring) {
        final var coords = ring.getCoordinates();
        for (final var coord : coords) {
            final var temp = coord.x;
            coord.x = coord.y;
            coord.y = temp;
        }
        return new LinearRing(new PackedCoordinateSequence.Double(coords), ring.getFactory());
    }

    public List<PointData<Object>> getPointData(final Coordinate coordinate) {
        return Arrays.stream(values())
                .flatMap(p -> getPointData(p, new Position2D(wgs84, coordinate.x, coordinate.y)))
                .toList();
    }
}

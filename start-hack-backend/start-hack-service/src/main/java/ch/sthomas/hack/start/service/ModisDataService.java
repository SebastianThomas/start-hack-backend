package ch.sthomas.hack.start.service;

import static ch.sthomas.hack.start.model.product.ModisProduct.*;

import static java.time.ZoneOffset.UTC;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;
import ch.sthomas.hack.start.model.points.PointData;
import ch.sthomas.hack.start.model.points.TimeLctStatData;
import ch.sthomas.hack.start.model.points.TimeNumericStatData;
import ch.sthomas.hack.start.model.points.TimeStatData;
import ch.sthomas.hack.start.model.product.ModisProduct;
import ch.sthomas.hack.start.model.util.MapCollectors;
import ch.sthomas.hack.start.service.geo.GridCoverageService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.*;

import org.geotools.api.coverage.PointOutsideCoverageException;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
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
            logger.info("Loading data for product {}", value);
            loadAndSaveData(value);
        }
    }

    private Path getPath(final ModisProduct product) {
        return switch (product) {
            case LCT -> modisLctFolder;
            case GP, GP_SIMPLIFIED -> modisGPFolder;
            case POPULATION_DENSITY -> populationDensityFolder;
            case CLIMATE_PRECIPITATION -> climatePrecipitationFolder;
        };
    }

    private IntFunction<String> productPathFilename(final ModisProduct product) {
        return year ->
                switch (product) {
                    case LCT -> year + "LCT.tif";
                    case GP, GP_SIMPLIFIED -> year + "_GP.tif";
                    case POPULATION_DENSITY -> "Assaba_Pop_" + year + ".tif";
                    case CLIMATE_PRECIPITATION -> year + "R.tif";
                };
    }

    public IntStream dataYearsStream() {
        return IntStream.range(2000, 2024);
    }

    public Map<Integer, GridCoverage2D> loadYearsRaster(final ModisProduct product) {
        final var path = getPath(product);
        final var productPathFilename = productPathFilename(product);
        return dataYearsStream()
                .<Optional<Map.Entry<Integer, GridCoverage2D>>>mapToObj(
                        year -> {
                            try {
                                final var filename = productPathFilename.apply(year);
                                final var productPath = path.resolve(filename);
                                return Optional.ofNullable(geoService.getTif(productPath))
                                        .map(t -> Map.entry(year, t));
                            } catch (final IOException e) {
                                return Optional.empty();
                            }
                        })
                .flatMap(Optional::stream)
                .collect(MapCollectors.entriesToMap());
    }

    public Map<Integer, BaseFeatureCollection> loadYears(final ModisProduct product) {
        final var path = getPath(product);
        final var productPathFilename = productPathFilename(product);
        final var gdalToFeature = gdalToFeature(product);
        return dataYearsStream()
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
    }

    public void loadAndSaveData(final ModisProduct product) {
        final var years = loadYears(product);
        logger.info("Saving data for product {} at years {}", product, years.keySet());
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
        final var path = getPath(product);
        final var productPathFilename = productPathFilename(product);
        return dataYearsStream()
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
                                                        final var rawVal =
                                                                product.mapPointEval()
                                                                        .apply(g.evaluate(point));
                                                        if (product == LCT) {
                                                            return getLandUseFromKey((int) rawVal);
                                                        }
                                                        return rawVal;
                                                    } catch (
                                                            final PointOutsideCoverageException e) {
                                                        logger.trace(
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

    public BaseFeatureCollection invertCoords(final BaseFeatureCollection featureCollection) {
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
        return Arrays.stream(ModisProduct.values())
                .flatMap(p -> getPointData(p, new Position2D(wgs84, coordinate.x, coordinate.y)))
                .toList();
    }

    public void loadSpacialAggregatedData() throws IOException {
        for (final var product : ModisProduct.values()) {
            final var result = product == LCT ? countLctLandUses() : getTimeData(product);
            objectMapper.writeValue(
                    outputFolder
                            .resolve("aggregated-" + product.name().toLowerCase() + ".json")
                            .toFile(),
                    result);
        }
    }

    private Map<Integer, TimeStatData> countLctLandUses() {
        final var grids = loadYearsRaster(LCT);
        return grids.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), countLctLandUses(entry.getValue())))
                .collect(MapCollectors.entriesToMap());
    }

    private TimeLctStatData countLctLandUses(final GridCoverage2D grid) {
        final var buf = (DataBufferByte) grid.getRenderedImage().getData().getDataBuffer();
        final var countsPerInt =
                Bytes.asList(buf.getData()).stream()
                        .map(Byte::intValue)
                        .collect(Collectors.toMap(Function.identity(), e -> 1, Integer::sum));
        return new TimeLctStatData(
                countsPerInt.entrySet().stream()
                        .filter(e -> e.getKey() > 0)
                        .map(
                                entry ->
                                        Map.entry(
                                                getLandUseFromKey(entry.getKey()),
                                                entry.getValue()))
                        .collect(MapCollectors.entriesToMap()));
    }

    private Map<Integer, TimeNumericStatData> getTimeData(final ModisProduct product) {
        final var grids = loadYearsRaster(product);
        if (Set.of(CLIMATE_PRECIPITATION, POPULATION_DENSITY, GP, GP_SIMPLIFIED)
                .contains(product)) {
            return grids.entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), aggregateSpace(entry.getValue())))
                    .collect(MapCollectors.entriesToMap());
        }
        throw new IllegalArgumentException(
                "Product " + product + " is not supported for Numeric Stat Data.");
    }

    private TimeNumericStatData aggregateSpace(final GridCoverage2D grid) {
        final var dataBuffer = grid.getRenderedImage().getData().getDataBuffer();
        final var stats =
                (switch (dataBuffer) {
                            case final DataBufferFloat f -> Floats.asList(f.getData());
                            case final DataBufferDouble d -> Doubles.asList(d.getData());
                            case final DataBufferInt i -> Ints.asList(i.getData());
                            case final DataBufferByte b -> Bytes.asList(b.getData());
                            case final DataBufferShort s -> Shorts.asList(s.getData());
                            default ->
                                    throw new IllegalArgumentException(
                                            MessageFormat.format(
                                                    "No data buffer with type {0} expected.",
                                                    dataBuffer.getDataType()));
                        })
                        .stream().mapToDouble(Number::doubleValue).summaryStatistics();
        return new TimeNumericStatData(
                stats.getMin(), stats.getMax(), stats.getAverage(), stats.getCount());
    }
}

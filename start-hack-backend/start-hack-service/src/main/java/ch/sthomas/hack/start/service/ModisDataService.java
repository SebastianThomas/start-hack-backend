package ch.sthomas.hack.start.service;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;
import ch.sthomas.hack.start.model.product.ModisProduct;
import ch.sthomas.hack.start.model.util.MapCollectors;
import ch.sthomas.hack.start.service.geo.GridCoverageService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

@Service
public class ModisDataService {
    private static final Logger logger = LoggerFactory.getLogger(ModisDataService.class);
    private final Path modisLctFolder;
    private final Path modisGPFolder;
    private final Path outputFolder;
    private final GeoService geoService;
    private final GridCoverageService gridCoverageService;
    private final ObjectMapper objectMapper;

    public ModisDataService(
            @Value("${ch.sthomas.hack.start.service.modis-lct.folder}") final String modisLctFolder,
            @Value("${ch.sthomas.hack.start.service.modis-gp.folder}") final String modisGPFolder,
            @Value("${ch.sthomas.hack.start.public.folder}") final String outputFolder,
            final GeoService geoService,
            final GridCoverageService gridCoverageService,
            final ObjectMapper objectMapper) {
        this.geoService = geoService;
        this.outputFolder = Paths.get(outputFolder);
        this.modisLctFolder = Path.of(modisLctFolder);
        this.modisGPFolder = Path.of(modisGPFolder);
        this.gridCoverageService = gridCoverageService;
        this.objectMapper = objectMapper;
    }

    public void loadAndSaveData() {
        loadAndSaveData(ModisProduct.LCT);
        loadAndSaveData(ModisProduct.GP);
    }

    public void loadAndSaveData(final ModisProduct product) {
        final var path =
                switch (product) {
                    case LCT -> modisLctFolder;
                    case GP -> modisGPFolder;
                };
        final var productPathFilenamePart =
                switch (product) {
                    case LCT -> "LCT.tif";
                    case GP -> "_GP.tif";
                };
        final var gdalToFeature = gdalToFeature(product);
        final var years =
                IntStream.range(2010, 2023)
                        .<Optional<Map.Entry<Integer, BaseFeatureCollection>>>mapToObj(
                                year -> {
                                    try {
                                        final var filename = year + productPathFilenamePart;
                                        final var productPath = path.resolve(filename);
                                        return Optional.ofNullable(geoService.readTif(productPath))
                                                .map(gridCoverageService::warpToWGS84)
                                                .map(gridCoverageService::vectorize)
                                                .map(gdalToFeature)
                                                .map(this::invertCoords)
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
}

package ch.sthomas.hack.start.service;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;
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
import java.util.stream.IntStream;

@Service
public class ModisDataService {
    private static final Logger logger = LoggerFactory.getLogger(ModisDataService.class);
    private final Path modisLctFolder;
    private final Path outputFolder;
    private final GeoService geoService;
    private final GridCoverageService gridCoverageService;
    private final ObjectMapper objectMapper;

    public ModisDataService(
            @Value("${ch.sthomas.hack.start.service.modis-lct.folder}") final String modisLctFolder,
            @Value("${ch.sthomas.hack.start.public.folder}") final String outputFolder,
            final GeoService geoService,
            final GridCoverageService gridCoverageService,
            final ObjectMapper objectMapper) {
        this.geoService = geoService;
        this.outputFolder = Paths.get(outputFolder);
        this.modisLctFolder = Path.of(modisLctFolder);
        this.gridCoverageService = gridCoverageService;
        this.objectMapper = objectMapper;
    }

    public void loadAndSaveData() {
        final var years =
                IntStream.range(2010, 2023)
                        .mapToObj(
                                year -> {
                                    try {
                                        return Map.entry(
                                                year,
                                                Optional.ofNullable(
                                                                geoService.readTif(
                                                                        modisLctFolder.resolve(
                                                                                year + "LCT.tif")))
                                                        .map(gridCoverageService::warpToWGS84)
                                                        .map(
                                                                g ->
                                                                        gridCoverageService
                                                                                .vectorize(
                                                                                        g, false))
                                                        .map(
                                                                collection ->
                                                                        new BaseFeatureCollection()
                                                                                .setFeatures(
                                                                                        collection
                                                                                                .getFeatures()
                                                                                                .stream()
                                                                                                .map(
                                                                                                        this
                                                                                                                ::invertCoordsAndFilterSmall)
                                                                                                .filter(
                                                                                                        Objects
                                                                                                                ::nonNull)
                                                                                                .toList())));
                                    } catch (final IOException e) {
                                        logger.info("Could not read Tif file and contour", e);
                                        return null;
                                    }
                                })
                        .filter(Objects::nonNull)
                        .collect(MapCollectors.entriesToMap());
        years.forEach(
                (year, vectors) -> {
                    final var outputFile =
                            outputFolder.resolve("lct-" + year + ".geojson").toFile();
                    outputFile.getParentFile().mkdirs();
                    try {
                        objectMapper.writeValue(outputFile, vectors);
                        logger.debug("Saved vectors to {}", outputFile.toPath().toAbsolutePath());
                    } catch (final IOException e) {
                        logger.info(e.getMessage());
                    }
                });
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
            case MultiPolygon p ->
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

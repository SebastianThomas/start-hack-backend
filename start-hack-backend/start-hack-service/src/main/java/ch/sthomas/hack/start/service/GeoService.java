package ch.sthomas.hack.start.service;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;
import ch.sthomas.hack.start.model.util.MapCollectors;
import ch.sthomas.hack.start.service.geo.GridCoverageService;
import ch.sthomas.hack.start.service.geo.shapefile.ShapefileParser;
import ch.sthomas.hack.start.service.geo.tif.TifParser;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.Nullable;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeoService {

    private final ObjectMapper objectMapper;

    private final Map<Path, GridCoverage2D> tifCache;
    private final GridCoverageService gridCoverageService;

    public GeoService(
            final ObjectMapper objectMapper, final GridCoverageService gridCoverageService) {
        this.objectMapper = objectMapper;
        this.tifCache = new ConcurrentHashMap<>();
        this.gridCoverageService = gridCoverageService;
    }

    public BaseFeatureCollection getFeatureCollection(final Path path) throws IOException {
        return objectMapper.readValue(path.toFile(), BaseFeatureCollection.class);
    }

    public Collection<SimpleFeature> readShape(final Path path, final String datasetName)
            throws IOException {
        return new ShapefileParser(path, datasetName).loadData();
    }

    private GridCoverage2D readTif(final Path path) throws IOException {
        return new TifParser(path).parse();
    }

    @Nullable
    public GridCoverage2D getTif(final Path path) throws IOException {
        if (tifCache.containsKey(path)) {
            return tifCache.get(path);
        }
        return Optional.ofNullable(readTif(path))
                .map(gridCoverageService::warpToWGS84)
                .map(
                        gc -> {
                            tifCache.put(path, gc);
                            return gc;
                        })
                .orElse(null);
    }

    public BaseFeature toFeature(final SimpleFeature feature) {
        return new BaseFeature()
                .setGeometry((Geometry) feature.getDefaultGeometry())
                .setType("Feature")
                .setId(
                        (String)
                                Objects.requireNonNullElse(
                                        feature.getID(), feature.getProperty("FID_1").getValue()))
                .setProperties(
                        feature.getProperties().stream()
                                .filter(
                                        property ->
                                                !"the_geom".equals(property.getName().toString()))
                                .filter(p -> p.getValue() != null)
                                .map(p -> Map.entry(p.getName().toString(), p.getValue()))
                                .collect(MapCollectors.entriesToMap()));
    }
}

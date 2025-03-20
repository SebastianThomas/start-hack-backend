package ch.sthomas.hack.start.service;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;
import ch.sthomas.hack.start.model.util.MapCollectors;
import ch.sthomas.hack.start.service.shapefile.ShapefileParser;
import ch.sthomas.hack.start.service.tif.TifParser;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

@Service
public class GeoService {

    private final ObjectMapper objectMapper;

    public GeoService(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public BaseFeatureCollection getFeatureCollection(final Path path) throws IOException {
        return objectMapper.readValue(path.toFile(), BaseFeatureCollection.class);
    }

    public Collection<SimpleFeature> readShape(final Path path, final String datasetName)
            throws IOException {
        return new ShapefileParser(path, datasetName).loadData();
    }

    public GridCoverage2D readTif(final Path path) throws IOException {
        return new TifParser(path).parse();
    }

    public BaseFeature toFeature(final SimpleFeature feature) {
        return new BaseFeature()
                .setGeometry((Geometry) feature.getDefaultGeometry())
                .setId(feature.getID())
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

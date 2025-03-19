package ch.sthomas.hack.start.service;

import ch.sthomas.hack.start.model.feature.BaseFeatureCollection;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.geotools.api.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

@Service
public class AdminDataService {
    private static final Logger logger = LoggerFactory.getLogger(AdminDataService.class);
    private final Path adminFolder;
    private final Path outputFolder;
    private final GeoService geoService;
    private final ObjectMapper objectMapper;

    public AdminDataService(
            @Value("${ch.sthomas.hack.start.service.admin.folder}") final String adminFolder,
            @Value("${ch.sthomas.hack.start.public.folder}") final String outputFolder,
            final GeoService geoService,
            final ObjectMapper objectMapper) {
        this.adminFolder = Path.of(adminFolder);
        this.outputFolder = Path.of(outputFolder);
        this.geoService = geoService;
        this.objectMapper = objectMapper;
    }

    public Collection<SimpleFeature> getDistricts() throws IOException {
        return geoService.readShape(adminFolder, "Assaba_Districts_layer");
    }

    public Collection<SimpleFeature> getRegions() throws IOException {
        return geoService.readShape(adminFolder, "Assaba_Region_layer");
    }

    public void loadAndSaveRegions() throws IOException {
        save(getDistricts(), "districts");
        save(getRegions(), "regions");
    }

    public void save(final Collection<SimpleFeature> simpleFeatures, final String collectionName)
            throws IOException {
        final var features = simpleFeatures.stream().map(geoService::toFeature).toList();
        final var outputFile = outputFolder.resolve(collectionName + ".geojson");
        outputFile.toFile().getParentFile().mkdirs();
        logger.info("Saving features to {}", outputFile);
        objectMapper.writeValue(
                outputFile.toFile(), new BaseFeatureCollection().setFeatures(features));
    }
}

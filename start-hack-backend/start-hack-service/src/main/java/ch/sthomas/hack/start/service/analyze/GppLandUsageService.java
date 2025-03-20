package ch.sthomas.hack.start.service.analyze;

import static ch.sthomas.hack.start.model.product.ModisProduct.GP;
import static ch.sthomas.hack.start.model.product.ModisProduct.LCT;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.model.feature.analyze.GppLandRankingFeature;
import ch.sthomas.hack.start.model.feature.analyze.GppLandRankingFeatureCollection;
import ch.sthomas.hack.start.model.feature.analyze.GppLandRankingFeatureProperties;
import ch.sthomas.hack.start.model.product.ModisProduct;
import ch.sthomas.hack.start.model.util.MapCollectors;
import ch.sthomas.hack.start.service.ModisDataService;
import ch.sthomas.hack.start.service.geo.GridCoverageService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pivovarit.function.ThrowingBiConsumer;

import org.apache.commons.lang3.tuple.Pair;
import org.geotools.api.coverage.PointOutsideCoverageException;
import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.Position2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

@Service
public class GppLandUsageService {
    private static final Logger logger = LoggerFactory.getLogger(GppLandUsageService.class);
    private final ModisDataService modisDataService;
    private final Path outputFolder;
    private final double gridSizeDegrees = 0.04; // <500m
    private final GridCoverageService gridCoverageService;
    private final ObjectMapper objectMapper;

    public GppLandUsageService(
            @Value("${ch.sthomas.hack.start.public.folder}") final String outputFolder,
            final ModisDataService modisDataService,
            final GridCoverageService gridCoverageService,
            final ObjectMapper objectMapper) {
        this.modisDataService = modisDataService;
        this.outputFolder = Path.of(outputFolder);
        this.gridCoverageService = gridCoverageService;
        this.objectMapper = objectMapper;
    }

    public void loadAndSaveUsageRanking() {
        final var rankings = getGppLandUsageRanking();
        rankings.forEach(
                ThrowingBiConsumer.unchecked(
                        (year, collection) ->
                                objectMapper.writeValue(
                                        outputFolder
                                                .resolve("gpp-ranking-" + year + ".geojson")
                                                .toFile(),
                                        collection)));
    }

    public Map<Integer, GppLandRankingFeatureCollection> getGppLandUsageRanking() {
        final var landUsagePerYearRaster = modisDataService.loadYearsRaster(LCT);
        final var gppPerYearRaster = modisDataService.loadYearsRaster(ModisProduct.GP);
        return modisDataService
                .dataYearsStream()
                .mapToObj(
                        year ->
                                getGppAnalyzeForYear(
                                        gppPerYearRaster, landUsagePerYearRaster, year))
                .filter(Objects::nonNull)
                .map(p -> Pair.of(p.getLeft(), gridCoverageService.vectorize(p.getRight())))
                .map(p -> Pair.of(p.getLeft(), modisDataService.invertCoords(p.getRight())))
                .map(
                        pair ->
                                Pair.of(
                                        pair.getLeft(),
                                        pair.getRight().getFeatures().stream()
                                                .map(this::toGppLandRankingFeature)
                                                .toList()))
                .map(
                        pair ->
                                Pair.of(
                                        pair.getLeft(),
                                        new GppLandRankingFeatureCollection()
                                                .setFeatures(pair.getRight())))
                .collect(MapCollectors.entriesToMap());
    }

    private GppLandRankingFeature toGppLandRankingFeature(final BaseFeature f) {
        return new GppLandRankingFeature()
                .setId(f.getId())
                .setType(f.getType())
                .setGeometry(f.getGeometry())
                .setProperties(new GppLandRankingFeatureProperties(f.getProperty("DN"), null));
    }

    private Pair<Integer, GridCoverage2D> getGppAnalyzeForYear(
            final Map<Integer, GridCoverage2D> gppPerYearRaster,
            final Map<Integer, GridCoverage2D> landUsagePerYearRaster,
            final int year) {
        final var gppRaster = gppPerYearRaster.get(year);
        final var landUsagePerYear = landUsagePerYearRaster.get(year);
        if (gppRaster == null) {
            return null;
        }
        final var envelope = gppRaster.getEnvelope2D();
        if (landUsagePerYear != null) {
            envelope.expandToInclude(landUsagePerYear.getEnvelope2D());
        }
        return Pair.of(
                year,
                generateGridCoverage(
                        envelope,
                        c -> {
                            if (landUsagePerYear != null) {
                                try {
                                    final var landUsageVal =
                                            LCT.<Integer>mapPointEval()
                                                    .apply(landUsagePerYear.evaluate(c));
                                    if (landUsageVal != 0 && landUsageVal > 13) {
                                        final var landUsage =
                                                ModisProduct.getLandUseFromKey(landUsageVal);
                                        logger.trace(
                                                "Ignore GPP on land usage value {}.", landUsage);
                                        return 0;
                                    }
                                } catch (final PointOutsideCoverageException ignored) {
                                }
                            }
                            try {
                                final var gpp =
                                        GP.<Integer>mapPointEval().apply(gppRaster.evaluate(c));
                                return gpp > 60000 ? -1 : (int) (gpp * 0.365 / (200) + 1);
                            } catch (final PointOutsideCoverageException e) {
                                return -1;
                            }
                        }));
    }

    public GridCoverage2D generateGridCoverage(
            final ReferencedEnvelope envelope, final ToIntFunction<Position> eval) {
        final var minX = envelope.getMinX();
        final var minY = envelope.getMinY();
        final var width = (int) Math.ceil(envelope.getWidth() / gridSizeDegrees);
        final var height = (int) Math.ceil(envelope.getHeight() / gridSizeDegrees);

        final var sampleModel =
                new ComponentSampleModel(
                        DataBuffer.TYPE_INT, width, height, 1, width, new int[] {0});
        final var dataBuffer = new DataBufferInt(new int[width * height], width * height);
        final var raster = Raster.createWritableRaster(sampleModel, dataBuffer, null);

        for (var i = 0; i < width; i++) {
            for (var j = 0; j < height; j++) {
                final var x = minX + i * gridSizeDegrees;
                final var y = minY + j * gridSizeDegrees;
                final var coord = new Position2D(x, y);

                final var value = eval.applyAsInt(coord);
                raster.setSample(i, j, 0, value);
            }
        }

        final var factory = new GridCoverageFactory();
        return factory.create("GridCoverage", raster, envelope);
    }
}

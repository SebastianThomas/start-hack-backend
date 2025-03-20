package ch.sthomas.hack.start.ws.config;

import ch.sthomas.hack.start.service.AdminDataService;
import ch.sthomas.hack.start.service.ModisDataService;
import ch.sthomas.hack.start.service.OSMDataService;
import ch.sthomas.hack.start.service.analyze.GppLandUsageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
public class WsSchedulingConfig {
    private static final Logger logger = LoggerFactory.getLogger(WsSchedulingConfig.class);
    private final AdminDataService adminDataService;
    private final ModisDataService modisDataService;
    private final OSMDataService osmDataService;
    private final GppLandUsageService gppLandUsageService;

    public WsSchedulingConfig(
            final AdminDataService adminDataService,
            final ModisDataService modisDataService,
            final OSMDataService osmDataService,
            GppLandUsageService gppLandUsageService) {
        this.adminDataService = adminDataService;
        this.modisDataService = modisDataService;
        this.osmDataService = osmDataService;
        this.gppLandUsageService = gppLandUsageService;
    }

    @Scheduled(initialDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void loadRegions() throws IOException {
        adminDataService.loadAndSaveRegions();
        logger.info("Administrative data loaded");
    }

    @Scheduled(initialDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void loadGrids() {
        modisDataService.loadAndSaveData();
        logger.info("MODIS Data Grids Loaded");
    }

    @Scheduled(initialDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void loadOSM() throws IOException {
        osmDataService.loadAndSaveData();
        logger.info("OSM data loaded");
    }

    @Scheduled(initialDelay = 2, timeUnit = TimeUnit.SECONDS)
    public void loadAnalyze() {
        gppLandUsageService.loadAndSaveUsageRanking();
        logger.info("Analysis ranking loaded");
    }
}

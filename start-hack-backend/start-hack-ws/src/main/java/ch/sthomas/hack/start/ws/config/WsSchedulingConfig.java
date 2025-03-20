package ch.sthomas.hack.start.ws.config;

import ch.sthomas.hack.start.service.AdminDataService;
import ch.sthomas.hack.start.service.ModisDataService;
import ch.sthomas.hack.start.service.OSMDataService;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
public class WsSchedulingConfig {
    private final AdminDataService adminDataService;
    private final ModisDataService modisDataService;
    private final OSMDataService osmDataService;

    public WsSchedulingConfig(
            final AdminDataService adminDataService,
            final ModisDataService modisDataService,
            final OSMDataService osmDataService) {
        this.adminDataService = adminDataService;
        this.modisDataService = modisDataService;
        this.osmDataService = osmDataService;
    }

    @Scheduled(initialDelay = 10, timeUnit = TimeUnit.SECONDS)
    public void loadRegions() throws IOException {
        adminDataService.loadAndSaveRegions();
    }

    @Scheduled(initialDelay = 20, timeUnit = TimeUnit.SECONDS)
    public void loadGrids() {
        modisDataService.loadAndSaveData();
    }

    @Scheduled(initialDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void loadOSM() throws IOException {
        osmDataService.loadAndSaveData();
    }
}

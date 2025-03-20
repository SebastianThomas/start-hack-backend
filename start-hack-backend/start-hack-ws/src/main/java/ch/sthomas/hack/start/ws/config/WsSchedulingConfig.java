package ch.sthomas.hack.start.ws.config;

import ch.sthomas.hack.start.service.AdminDataService;
import ch.sthomas.hack.start.service.ModisDataService;

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

    public WsSchedulingConfig(
            final AdminDataService adminDataService, ModisDataService modisDataService) {
        this.adminDataService = adminDataService;
        this.modisDataService = modisDataService;
    }

    @Scheduled(initialDelay = 2, timeUnit = TimeUnit.SECONDS)
    public void loadRegions() throws IOException {
        adminDataService.loadAndSaveRegions();
    }

    @Scheduled(initialDelay = 2, timeUnit = TimeUnit.SECONDS)
    public void loadLCT() {
        modisDataService.loadAndSaveData();
    }
}

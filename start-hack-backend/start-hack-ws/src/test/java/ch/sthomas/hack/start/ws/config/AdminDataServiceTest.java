package ch.sthomas.hack.start.ws.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.sthomas.hack.start.model.feature.BaseFeature;
import ch.sthomas.hack.start.service.AdminDataService;
import ch.sthomas.hack.start.service.GeoService;
import ch.sthomas.hack.start.service.geo.GridCoverageService;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@SpringBootTest(
        classes = {
            AdminDataService.class,
            GeoService.class,
            GridCoverageService.class,
            WsBaseConfig.class
        })
@ActiveProfiles("test")
class AdminDataServiceTest {
    @MockitoBean private EntityManagerFactory entityManagerFactory;
    @Autowired private AdminDataService adminDataService;
    @Autowired private GeoService geoService;
    @Autowired private GridCoverageService gridCoverageService;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void testDistricts() throws IOException {
        final var districts = adminDataService.getDistricts();
        assertNotNull(districts);
        assertEquals(26, districts.size());
        assertNotNull(districts.stream().map(geoService::toFeature).toList());
    }

    @Test
    void test() throws IOException {
        final var regions = adminDataService.getRegions();
        assertNotNull(regions);
        assertEquals(5, regions.size());
        assertNotNull(regions.stream().map(geoService::toFeature).toList());
    }

    @Test
    void jtsGeometryRoundTripsThroughJackson() {
        final var feature =
                objectMapper.readValue(
                        """
                        {"type":"Feature","geometry":{"type":"Point","coordinates":[8.54,47.37]}}
                        """,
                        BaseFeature.class);

        assertEquals("POINT (8.54 47.37)", feature.getGeometry().toText());
        assertEquals(
                feature.getGeometry(),
                objectMapper.readValue(
                                objectMapper.writeValueAsString(feature), BaseFeature.class)
                        .getGeometry());
    }
}

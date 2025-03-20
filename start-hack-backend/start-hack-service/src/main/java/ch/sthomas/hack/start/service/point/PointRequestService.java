package ch.sthomas.hack.start.service.point;

import ch.sthomas.hack.start.model.points.PointData;
import ch.sthomas.hack.start.service.AdminDataService;
import ch.sthomas.hack.start.service.ModisDataService;

import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointRequestService {
    private final ModisDataService modisDataService;
    private final AdminDataService adminDataService;

    public PointRequestService(
            final ModisDataService modisDataService, final AdminDataService adminDataService) {
        this.modisDataService = modisDataService;
        this.adminDataService = adminDataService;
    }

    public List<PointData<Object>> getPointData(final Coordinate coordinate) {
        return modisDataService.getPointData(coordinate);
    }
}

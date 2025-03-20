package ch.sthomas.hack.start.ws.controller;

import ch.sthomas.hack.start.model.points.PointData;
import ch.sthomas.hack.start.service.AdminDataService;
import ch.sthomas.hack.start.service.point.PointRequestService;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1")
@Validated
public class StartHackController {

    private static final Logger logger = LoggerFactory.getLogger(StartHackController.class);

    private final AdminDataService todoService;
    private final PointRequestService pointRequestService;

    public StartHackController(
            final AdminDataService todoService, PointRequestService pointRequestService) {
        this.todoService = todoService;
        this.pointRequestService = pointRequestService;
    }

    @Operation(summary = "Hello Endpoint")
    @GetMapping(path = "")
    public Hello hello() {
        return new Hello("Hello from start-hack WS", Instant.now());
    }

    @Operation(summary = "Get Data for a Point")
    @GetMapping(path = "/point-data")
    public List<PointData<Object>> getPointData(
            @RequestParam @Valid final int x, @RequestParam @Valid final int y) {
        return pointRequestService.getPointData(new Coordinate(x, y));
    }

    public record Hello(String msg, Instant time) {}
}

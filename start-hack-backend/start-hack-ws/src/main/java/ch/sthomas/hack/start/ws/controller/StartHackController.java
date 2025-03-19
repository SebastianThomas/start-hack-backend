package ch.sthomas.hack.start.ws.controller;

import ch.sthomas.hack.start.service.AdminDataService;

import io.swagger.v3.oas.annotations.Operation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/v1")
public class StartHackController {

    private static final Logger logger = LoggerFactory.getLogger(StartHackController.class);

    private final AdminDataService todoService;

    public StartHackController(final AdminDataService todoService) {
        this.todoService = todoService;
    }

    @Operation(summary = "Hello Endpoint")
    @GetMapping(path = "")
    public Hello hello() {
        return new Hello("Hello from start-hack WS", Instant.now());
    }

    public record Hello(String msg, Instant time) {}
}

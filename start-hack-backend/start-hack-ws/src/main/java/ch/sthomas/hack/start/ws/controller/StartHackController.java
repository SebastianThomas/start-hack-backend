package ch.sthomas.hack.start.ws.controller;

import ch.sthomas.hack.start.service.TodoService;

import io.swagger.v3.oas.annotations.Operation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class StartHackController {

    private static final Logger logger = LoggerFactory.getLogger(StartHackController.class);

    private final TodoService todoService;

    public StartHackController(final TodoService todoService) {
        this.todoService = todoService;
    }

    @Operation(summary = "Hello Endpoint")
    @GetMapping(path = "")
    public String hello() {
        return "Hello from start-hack WS";
    }
}

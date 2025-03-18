package ch.sthomas.hack.start.service;

import ch.sthomas.hack.start.data.service.TodoDataService;

import org.springframework.stereotype.Service;

@Service
public class TodoService {

    private final TodoDataService todoDataService;

    public TodoService(final TodoDataService todoDataService) {
        this.todoDataService = todoDataService;
    }
}

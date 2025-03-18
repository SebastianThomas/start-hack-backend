package ch.sthomas.hack.start.data.service;

import ch.sthomas.hack.start.data.repository.TodoRepository;
import org.springframework.stereotype.Service;

@Service
public class TodoDataService {

    private final TodoRepository todoRepository;

    public TodoDataService(
            final TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }
}

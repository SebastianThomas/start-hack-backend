package ch.sthomas.hack.start.data.repository;

import ch.sthomas.hack.start.model.entity.TodoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TodoRepository
        extends JpaRepository<TodoEntity, Integer> {}

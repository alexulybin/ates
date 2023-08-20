package ru.toughdev.ates.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.toughdev.ates.analytics.model.Task;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Task getByPublicId(UUID publicId);
}

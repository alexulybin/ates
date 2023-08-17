package ru.toughdev.ates.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.toughdev.ates.account.model.Task;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Task getByPublicId(UUID publicId);
}

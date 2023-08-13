package ru.toughdev.ates.tasktracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.toughdev.ates.tasktracker.model.Task;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, String> {

    Task getByPublicIdAndAssigneeId(UUID publicId, UUID assigneeId);
    List<Task> getByAssigneeId(UUID assigneeId);
}

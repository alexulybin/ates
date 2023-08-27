package ru.toughdev.ates.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.toughdev.ates.analytics.model.Task;
import ru.toughdev.ates.analytics.repository.TaskRepository;
import ru.toughdev.ates.analytics.repository.UserRepository;
import ru.toughdev.ates.event.task.TaskCompletedEventV1;
import ru.toughdev.ates.event.task.TaskCreatedEventV1;
import ru.toughdev.ates.event.task.TaskCreatedEventV2;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskEventHandler {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public void handle(TaskCreatedEventV1 event) {
        var user = userRepository.getByPublicId(UUID.fromString(event.getAssigneeId()));
        var task = Task.builder()
                .publicId(UUID.fromString(event.getPublicId()))
                .assigneeId(user.getId())
                .description(event.getDescription())
                .fee(event.getFee())
                .reward(event.getReward())
                .build();

        taskRepository.save(task);
        log.info("Task created " + task.getPublicId());
    }

    public void handle(TaskCreatedEventV2 event) {
        var user = userRepository.getByPublicId(UUID.fromString(event.getAssigneeId()));
        var task = Task.builder()
                .publicId(UUID.fromString(event.getPublicId()))
                .assigneeId(user.getId())
                .description(event.getDescription())
                .fee(event.getFee())
                .reward(event.getReward())
                .build();

        taskRepository.save(task);
        log.info("Task created " + task.getPublicId());
    }

    public void handle(TaskCompletedEventV1 event) {
        var task = taskRepository.getByPublicId(UUID.fromString(event.getPublicId()));
        task.setCompleted(true);

        taskRepository.save(task);
        log.info("Task updated " + task.getPublicId());
    }
}

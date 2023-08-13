package ru.toughdev.ates.tasktracker.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.toughdev.ates.tasktracker.dto.CreateTaskDto;
import ru.toughdev.ates.tasktracker.kafka.MessageProducer;
import ru.toughdev.ates.tasktracker.kafka.TaskEvent;
import ru.toughdev.ates.tasktracker.model.Task;
import ru.toughdev.ates.tasktracker.model.User;
import ru.toughdev.ates.tasktracker.repository.TaskRepository;
import ru.toughdev.ates.tasktracker.repository.UserRepository;
import ru.toughdev.ates.tasktracker.security.JwtUser;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
public class TaskController {

    private final int minFee = 10;
    private final int maxFee = 20;
    private final int minReward = 20;
    private final int maxReward = 40;

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final MessageProducer messageProducer;

    @PostMapping
    public @ResponseBody Task createTask(@RequestBody CreateTaskDto dto) throws JsonProcessingException {
        var random = new Random();

        var user = getAssignee();
        var task = Task.builder()
                .assigneeId(user.getId())
                .description(dto.getDescription())
                .fee(random.nextInt(maxFee - minFee) + minFee)
                .reward(random.nextInt(maxReward - minReward) + minReward)
                .build();
        var saved = taskRepository.saveAndFlush(task);

        var event = TaskEvent.builder()
                .eventType("TaskCreated")
                .publicId(saved.getPublicId())
                .description(saved.getDescription())
                .assigneeId(user.getPublicId())
                .fee(saved.getFee())
                .reward(saved.getReward())
                .build();
        messageProducer.sendMessage(event, "task-stream");

        event = TaskEvent.builder()
                .eventType("TaskAssigned")
                .publicId(saved.getPublicId())
                .assigneeId(user.getPublicId())
                .build();
        messageProducer.sendMessage(event, "task-lifecycle");

        log.info("Task created " + saved);

        return saved;
    }

    private User getAssignee() {
        var random = new Random();
        var users = userRepository.findByRoleNotIn(List.of("admin", "manager"));
        var num = random.nextInt(users.size());
        return users.get(num);
    }

    @PostMapping("/complete/{taskId}")
    public void completeTask(@PathVariable UUID taskId, Authentication authentication) throws JsonProcessingException {
        var userPublicId = ((JwtUser) authentication.getPrincipal()).getPublicId();
        var task = taskRepository.getByPublicIdAndAssigneeId(taskId, userPublicId);
        if (task != null) {
            task.setCompleted(true);
            taskRepository.saveAndFlush(task);

            var event = TaskEvent.builder()
                    .eventType("TaskCompleted")
                    .publicId(taskId)
                    .assigneeId(userPublicId)
                    .build();
            messageProducer.sendMessage(event, "task-lifecycle");

            log.info("Task completed " + task);
        } else {
            log.info("Task not found. taskId: " + taskId + " assigneeId: " + userPublicId);
        }
    }

    @PreAuthorize("hasAuthority('admin') or hasAuthority('manager')")
    @PostMapping("/reassign-all")
    public void reassignAllTasks() throws JsonProcessingException {
        var random = new Random();
        var users = userRepository.findByRoleNotIn(List.of("admin", "manager"));

        var tasks = taskRepository.findAll();
        for (var task : tasks) {
            var num = random.nextInt(users.size());
            var userId = users.get(num).getId();

            task.setAssigneeId(userId);
        }
        taskRepository.saveAllAndFlush(tasks);

        for (var task : tasks) {
            var event = TaskEvent.builder()
                    .eventType("TaskReassigned")
                    .publicId(task.getPublicId())
                    .assigneeId(users.stream()
                            .filter(us -> us.getId().equals(task.getAssigneeId()))
                            .findFirst()
                            .orElseThrow().getPublicId()
                    )
                    .build();
            messageProducer.sendMessage(event, "task-lifecycle");
        }
        log.info("All tasks reassigned");
    }

    @GetMapping("/my-tasks")
    public @ResponseBody List<Task> getMyTasks(Authentication authentication) {
        var userPublicId = ((JwtUser) authentication.getPrincipal()).getPublicId();
        return taskRepository.getByAssigneeId(userPublicId);
    }
}

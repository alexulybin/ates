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
import ru.toughdev.ates.event.task.TaskEventV1;
import ru.toughdev.ates.tasktracker.dto.CreateTaskDto;
import ru.toughdev.ates.tasktracker.kafka.MessageProducer;
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

        var fee = random.nextInt(maxFee - minFee) + minFee;
        var reward = random.nextInt(maxReward - minReward) + minReward;

        var user = getAssignee();
        var task = Task.builder()
                .assigneeId(user.getId())
                .description(dto.getDescription())
                .fee(Integer.valueOf(fee).longValue())
                .reward(Integer.valueOf(reward).longValue())
                .build();
        var saved = taskRepository.saveAndFlush(task);

        var event = TaskEventV1.newBuilder()
                .setEventType("TaskCreated")
                .setPublicId(saved.getPublicId().toString())
                .setDescription(saved.getDescription())
                .setAssigneeId(user.getPublicId().toString())
                .setFee(saved.getFee())
                .setReward(saved.getReward())
                .build();
        messageProducer.sendMessage(event, "task-stream");

        event = TaskEventV1.newBuilder()
                .setEventType("TaskAssigned")
                .setPublicId(saved.getPublicId().toString())
                .setAssigneeId(user.getPublicId().toString())
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
    public void completeTask(@PathVariable String taskId, Authentication authentication) throws JsonProcessingException {
        var login = ((JwtUser) authentication.getPrincipal()).getUsername();
        var user = userRepository.getByLogin(login);
        var task = taskRepository.getByPublicIdAndAssigneeId(UUID.fromString(taskId), user.getId());

        if (task != null) {
            task.setCompleted(true);
            taskRepository.saveAndFlush(task);

            var eventUpdated = TaskEventV1.newBuilder()
                    .setEventType("TaskUpdated")
                    .setPublicId(taskId)
                    .setCompleted(true)
                    .build();
            messageProducer.sendMessage(eventUpdated, "task-stream");

            var eventCompleted = TaskEventV1.newBuilder()
                    .setEventType("TaskCompleted")
                    .setPublicId(taskId)
                    .setAssigneeId(user.getPublicId().toString())
                    .build();
            messageProducer.sendMessage(eventCompleted, "task-lifecycle");

            log.info("Task completed " + task);
        } else {
            log.info("Task not found. taskId: " + taskId + " assigneeId: " + user.getPublicId());
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
            var event = TaskEventV1.newBuilder()
                    .setEventType("TaskReassigned")
                    .setPublicId(task.getPublicId().toString())
                    .setAssigneeId(users.stream()
                            .filter(us -> us.getId().equals(task.getAssigneeId()))
                            .findFirst()
                            .orElseThrow().getPublicId().toString()
                    )
                    .build();
            messageProducer.sendMessage(event, "task-lifecycle");
        }
        log.info("All tasks reassigned");
    }

    @GetMapping("/my-tasks")
    public @ResponseBody List<Task> getMyTasks(Authentication authentication) {
        var login = ((JwtUser) authentication.getPrincipal()).getUsername();
        var user = userRepository.getByLogin(login);
        return taskRepository.getByAssigneeId(user.getId());
    }
}

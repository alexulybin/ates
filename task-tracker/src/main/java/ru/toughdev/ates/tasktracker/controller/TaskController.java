package ru.toughdev.ates.tasktracker.controller;

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
import ru.toughdev.ates.event.task.TaskAddedEventV1;
import ru.toughdev.ates.event.task.TaskAddedEventV2;
import ru.toughdev.ates.event.task.TaskCompletedEventV1;
import ru.toughdev.ates.event.task.TaskCreatedEventV1;
import ru.toughdev.ates.event.task.TaskCreatedEventV2;
import ru.toughdev.ates.event.task.TaskReassignedEventV1;
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

    /**
     * Старая реализация добавления таски с отправкой события V1
     * Оставлено для тестирования
     */
    @PostMapping("/v1")
    public @ResponseBody Task addTask(@RequestBody CreateTaskDto dto) {
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

        var eventCreated = TaskCreatedEventV1.newBuilder()
                .setEventType("TaskCreated")
                .setPublicId(saved.getPublicId().toString())
                .setDescription(saved.getDescription())
                .setAssigneeId(user.getPublicId().toString())
                .setFee(saved.getFee())
                .setReward(saved.getReward())
                .build();
        messageProducer.sendMessage(eventCreated, "task-stream");

        log.info("Task created " + saved);

        var eventAdded = TaskAddedEventV1.newBuilder()
                .setEventType("TaskAdded")
                .setPublicId(saved.getPublicId().toString())
                .setDescription(saved.getDescription())
                .setAssigneeId(user.getPublicId().toString())
                .setFee(saved.getFee())
                .setReward(saved.getReward())
                .build();
        messageProducer.sendMessage(eventAdded, "task-lifecycle");

        log.info("Task added " + saved);

        return saved;
    }

    /**
     * Добавление таски с отправкой события V2
     */
    @PostMapping
    public @ResponseBody Task addTaskV2(@RequestBody CreateTaskDto dto) {
        var random = new Random();

        var fee = random.nextInt(maxFee - minFee) + minFee;
        var reward = random.nextInt(maxReward - minReward) + minReward;

        var jiraId = getJiraId(dto.getDescription());

        var user = getAssignee();
        var task = Task.builder()
                .assigneeId(user.getId())
                .description(dto.getDescription())
                .jiraId(jiraId)
                .fee(Integer.valueOf(fee).longValue())
                .reward(Integer.valueOf(reward).longValue())
                .build();
        var saved = taskRepository.saveAndFlush(task);

        var eventCreated = TaskCreatedEventV2.newBuilder()
                .setEventType("TaskCreated")
                .setPublicId(saved.getPublicId().toString())
                .setDescription(saved.getDescription())
                .setJiraId(jiraId)
                .setAssigneeId(user.getPublicId().toString())
                .setFee(saved.getFee())
                .setReward(saved.getReward())
                .build();
        messageProducer.sendMessage(eventCreated, "task-stream");

        var eventAdded = TaskAddedEventV2.newBuilder()
                .setEventType("TaskAdded")
                .setPublicId(saved.getPublicId().toString())
                .setDescription(saved.getDescription())
                .setJiraId(jiraId)
                .setAssigneeId(user.getPublicId().toString())
                .setFee(saved.getFee())
                .setReward(saved.getReward())
                .build();
        messageProducer.sendMessage(eventAdded, "task-lifecycle");

        log.info("Task created " + saved);

        return saved;
    }

    private String getJiraId(String description) {
        var ind1 = description.indexOf("[");
        var ind2 = description.indexOf("]");

        return description.substring(ind1+1, ind2);
    }

    private User getAssignee() {
        var random = new Random();
        var users = userRepository.findByRoleNotIn(List.of("admin", "manager"));
        var num = random.nextInt(users.size());
        return users.get(num);
    }

    @PostMapping("/complete/{taskId}")
    public void completeTask(@PathVariable String taskId, Authentication authentication) {
        var login = ((JwtUser) authentication.getPrincipal()).getUsername();
        var user = userRepository.getByLogin(login);
        var task = taskRepository.getByPublicIdAndAssigneeId(UUID.fromString(taskId), user.getId());

        if (task != null) {
            task.setCompleted(true);
            taskRepository.saveAndFlush(task);

            var eventCompleted = TaskCompletedEventV1.newBuilder()
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
    public void reassignAllTasks() {
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
            var event = TaskReassignedEventV1.newBuilder()
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

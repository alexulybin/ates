package ru.toughdev.ates.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.toughdev.ates.account.kafka.MessageProducer;
import ru.toughdev.ates.account.model.Payment;
import ru.toughdev.ates.account.model.Task;
import ru.toughdev.ates.account.repository.PaymentRepository;
import ru.toughdev.ates.account.repository.TaskRepository;
import ru.toughdev.ates.account.repository.UserRepository;
import ru.toughdev.ates.event.payment.PaymentMadeEventV1;
import ru.toughdev.ates.event.task.TaskAddedEventV1;
import ru.toughdev.ates.event.task.TaskAddedEventV2;
import ru.toughdev.ates.event.task.TaskCompletedEventV1;
import ru.toughdev.ates.event.task.TaskCreatedEventV1;
import ru.toughdev.ates.event.task.TaskCreatedEventV2;
import ru.toughdev.ates.event.task.TaskReassignedEventV1;

import java.time.LocalDateTime;
import java.util.TimeZone;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskEventHandler {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PaymentRepository paymentRepository;
    private final MessageProducer messageProducer;

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

    // код для обработки V2 отличается как минимум сохранением jiraId
    public void handle(TaskCreatedEventV2 event) {
        var user = userRepository.getByPublicId(UUID.fromString(event.getAssigneeId()));
        var task = Task.builder()
                .publicId(UUID.fromString(event.getPublicId()))
                .assigneeId(user.getId())
                .description(event.getDescription())
                .jiraId(event.getJiraId())
                .fee(event.getFee())
                .reward(event.getReward())
                .build();
        taskRepository.save(task);
        log.info("Task created " + task.getPublicId());
    }

    public void handle(TaskAddedEventV1 event) {
        var user = userRepository.getByPublicId(UUID.fromString(event.getAssigneeId()));
        var task = taskRepository.getByPublicId(UUID.fromString(event.getPublicId()));

        var payment = savePayment(task.getId(), user.getId(), task.getFee(), "fee");

        sendPaymentEvent("payment-lifecycle", "PaymentMade",
                task.getPublicId(), user.getPublicId(), null, payment.getType());

        log.info("Fee paid " + payment.getTaskId());
    }

    public void handle(TaskAddedEventV2 event) {
        var user = userRepository.getByPublicId(UUID.fromString(event.getAssigneeId()));
        var task = taskRepository.getByPublicId(UUID.fromString(event.getPublicId()));

        var payment = savePayment(task.getId(), user.getId(), task.getFee(), "fee");

        sendPaymentEvent("payment-lifecycle", "PaymentMade",
                task.getPublicId(), user.getPublicId(), null, payment.getType());

        log.info("Fee paid " + payment.getTaskId());
    }

    public void handle(TaskReassignedEventV1 event) {
        var user = userRepository.getByPublicId(UUID.fromString(event.getAssigneeId()));
        var task = taskRepository.getByPublicId(UUID.fromString(event.getPublicId()));

        var payment = savePayment(task.getId(), user.getId(), task.getFee(), "fee");

        sendPaymentEvent("payment-lifecycle", "PaymentMade",
                task.getPublicId(), user.getPublicId(), null, payment.getType());

        log.info("Fee paid " + payment.getTaskId());
    }

    public void handle(TaskCompletedEventV1 event) {
        var user = userRepository.getByPublicId(UUID.fromString(event.getAssigneeId()));
        var task = taskRepository.getByPublicId(UUID.fromString(event.getPublicId()));

        var payment = savePayment(task.getId(), user.getId(), task.getReward(), "reward");

        sendPaymentEvent("payment-lifecycle", "PaymentMade",
                task.getPublicId(), user.getPublicId(), null, payment.getType());

        log.info("Reward paid " + payment.getTaskId());
    }

    private Payment savePayment(Long taskId, Long userId, Long amount, String type) {
        var payment = Payment.builder()
                .taskId(taskId)
                .userId(userId)
                .amount(amount)
                .type(type)
                .dateTime(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }

    private void sendPaymentEvent(String topic, String eventType, UUID taskPublicId, UUID userPublicId,
                                  Long amount, String type) {

        var ldt = LocalDateTime.now();
        var datetime = ldt.atZone(TimeZone.getTimeZone("UTC").toZoneId()).toInstant().toEpochMilli();

        var event = PaymentMadeEventV1.newBuilder()
                .setEventType(eventType)
                .setTaskPublicId(taskPublicId.toString())
                .setUserPublicId(userPublicId.toString())
                .setAmount(amount)
                .setType(type)
                .setDateTime(datetime)
                .build();

        messageProducer.sendMessage(event, topic);
    }
}

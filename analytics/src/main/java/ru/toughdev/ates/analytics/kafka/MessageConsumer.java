package ru.toughdev.ates.analytics.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.toughdev.ates.analytics.model.Account;
import ru.toughdev.ates.analytics.model.Payment;
import ru.toughdev.ates.analytics.model.Task;
import ru.toughdev.ates.analytics.model.User;
import ru.toughdev.ates.analytics.repository.AccountRepository;
import ru.toughdev.ates.analytics.repository.PaymentRepository;
import ru.toughdev.ates.analytics.repository.TaskRepository;
import ru.toughdev.ates.analytics.repository.UserRepository;
import ru.toughdev.ates.event.account.AccountEventV1;
import ru.toughdev.ates.event.payment.PaymentEventV1;
import ru.toughdev.ates.event.task.TaskEventV1;
import ru.toughdev.ates.event.task.TaskEventV2;
import ru.toughdev.ates.event.user.UserEventV1;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    @KafkaListener(topics = "user-stream")
    public void receiveUserStreamMessage(@Payload UserEventV1 event) {
        log.info("Message received : " + event);

        if (event.getEventType().equals("UserCreated")) {
            var user = User.builder()
                    .publicId(UUID.fromString(event.getPublicId()))
                    .login(event.getLogin())
                    .email(event.getEmail())
                    .role(event.getRole())
                    .build();

            var savedUser = userRepository.save(user);
            log.info("User created " + user.getLogin());

            var account = Account.builder()
                    .userId(savedUser.getId())
                    .build();

            accountRepository.save(account);
            log.info("Account created for user" + user.getLogin());
        }
    }

    @KafkaListener(topics = "task-stream")
    public void receiveTaskStreamMessage(@Payload SpecificRecord record) {
        log.info("Message received : " + record);

        if (record instanceof TaskEventV1) {
            handleTaskStreamEvent((TaskEventV1) record);
        }

        if (record instanceof TaskEventV2) {
            handleTaskStreamEvent((TaskEventV2) record);
        }
    }

    private void handleTaskStreamEvent(TaskEventV1 event) {
        switch (event.getEventType()) {
            case "TaskCreated":
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
                break;

            case "TaskUpdated":
                task = taskRepository.getByPublicId(UUID.fromString(event.getPublicId()));
                var assigneeId = event.getAssigneeId() == null ? null :
                        userRepository.getByPublicId(UUID.fromString(event.getAssigneeId())).getId();

                task.setCompleted(Optional.ofNullable(event.getCompleted()).orElse(task.getCompleted()));
                task.setAssigneeId(Optional.ofNullable(assigneeId).orElse(task.getAssigneeId()));
                task.setDescription(Optional.ofNullable(event.getDescription()).orElse(task.getDescription()));

                taskRepository.save(task);
                log.info("Task updated " + task.getPublicId());
                break;
        }
    }

    // код для обработки V1 и V2 в данном случае пректически иденичный
    // но в общем случае он может отличаться
    private void handleTaskStreamEvent(TaskEventV2 event) {
        switch (event.getEventType()) {
            case "TaskCreated":
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
                break;

            case "TaskUpdated":
                task = taskRepository.getByPublicId(UUID.fromString(event.getPublicId()));
                var assigneeId = event.getAssigneeId() == null ? null :
                        userRepository.getByPublicId(UUID.fromString(event.getAssigneeId())).getId();

                task.setCompleted(Optional.ofNullable(event.getCompleted()).orElse(task.getCompleted()));
                task.setAssigneeId(Optional.ofNullable(assigneeId).orElse(task.getAssigneeId()));
                task.setDescription(Optional.ofNullable(event.getDescription()).orElse(task.getDescription()));

                taskRepository.save(task);
                log.info("Task updated " + task.getPublicId());
                break;
        }
    }

    @KafkaListener(topics = "payment-stream")
    public void receivePaymentStreamMessage(@Payload PaymentEventV1 event) {
        log.info("Message received : " + event);

        if (event.getEventType().equals("PaymentCreated")) {
            var user = userRepository.getByPublicId(UUID.fromString(event.getUserPublicId()));
            var task = taskRepository.getByPublicId(UUID.fromString(event.getPublicId()));

            var dateTime =
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDateTime()),
                            TimeZone.getTimeZone("UTC").toZoneId());

            var paymentFee = Payment.builder()
                    .publicId(UUID.fromString(event.getPublicId()))
                    .taskId(task.getId())
                    .userId(user.getId())
                    .amount(event.getAmount())
                    .type(event.getType())
                    .dateTime(dateTime)
                    .build();

            paymentRepository.save(paymentFee);
            log.info("PaymentCreated " + event.getPublicId());
        }
    }

    @KafkaListener(topics = "account-stream")
    public void receiveAccountStreamMessage(@Payload AccountEventV1 event) {
        log.info("Message received : " + event);

        if (event.getEventType().equals("AccountUpdated")) {
            var user = userRepository.getByPublicId(UUID.fromString(event.getUserPublicId()));
            var account = accountRepository.getByUserId(user.getId());

            account.setBalance(event.getBalance());
            accountRepository.save(account);
        }
    }
}

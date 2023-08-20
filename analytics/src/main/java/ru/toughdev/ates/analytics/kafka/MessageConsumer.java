package ru.toughdev.ates.analytics.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import ru.toughdev.ates.analytics.kafka.event.AccountEvent;
import ru.toughdev.ates.analytics.kafka.event.PaymentEvent;
import ru.toughdev.ates.analytics.kafka.event.TaskEvent;
import ru.toughdev.ates.analytics.kafka.event.UserEvent;
import ru.toughdev.ates.analytics.model.Account;
import ru.toughdev.ates.analytics.model.Payment;
import ru.toughdev.ates.analytics.model.Task;
import ru.toughdev.ates.analytics.model.User;
import ru.toughdev.ates.analytics.repository.AccountRepository;
import ru.toughdev.ates.analytics.repository.PaymentRepository;
import ru.toughdev.ates.analytics.repository.TaskRepository;
import ru.toughdev.ates.analytics.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    @KafkaListener(topics = "user-stream")
    public void receiveUserStreamMessage(@Payload String message) throws JsonProcessingException {
        log.info("Message received : " + message);

        var mapper = new ObjectMapper();
        var event = mapper.readValue(message, UserEvent.class);

        if (event.getEventType().equals("UserCreated")) {
            var user = User.builder()
                    .publicId(event.getPublicId())
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
    public void receiveTaskStreamMessage(@Payload String message) throws JsonProcessingException {
        log.info("Message received : " + message);

        var mapper = new ObjectMapper();
        var event = mapper.readValue(message, TaskEvent.class);

        switch (event.getEventType()) {
            case "TaskCreated":
                var user = userRepository.getByPublicId(event.getAssigneeId());
                var task = Task.builder()
                        .publicId(event.getPublicId())
                        .assigneeId(user.getId())
                        .description(event.getDescription())
                        .fee(event.getFee())
                        .reward(event.getReward())
                        .build();

                taskRepository.save(task);
                log.info("Task created " + task.getPublicId());
                break;

            case "TaskUpdated":
                task = taskRepository.getByPublicId(event.getPublicId());
                var assigneeId = event.getAssigneeId() == null ? null :
                        userRepository.getByPublicId(event.getAssigneeId()).getId();

                task.setCompleted(Optional.ofNullable(event.getCompleted()).orElse(task.getCompleted()));
                task.setAssigneeId(Optional.ofNullable(assigneeId).orElse(task.getAssigneeId()));
                task.setDescription(Optional.ofNullable(event.getDescription()).orElse(task.getDescription()));

                taskRepository.save(task);
                log.info("Task updated " + task.getPublicId());
                break;
        }
    }

    @KafkaListener(topics = "payment-stream")
    public void receivePaymentStreamMessage(@Payload String message) throws JsonProcessingException {
        log.info("Message received : " + message);

        var mapper = new ObjectMapper();
        var event = mapper.readValue(message, PaymentEvent.class);

        if (event.getEventType().equals("PaymentCreated")) {
            var user = userRepository.getByPublicId(event.getUserPublicId());
            var task = taskRepository.getByPublicId(event.getPublicId());

            var paymentFee = Payment.builder()
                    .publicId(event.getPublicId())
                    .taskId(task.getId())
                    .userId(user.getId())
                    .amount(event.getAmount())
                    .type(event.getType())
                    .dateTime(event.getDateTime())
                    .build();

            paymentRepository.save(paymentFee);
            log.info("PaymentCreated " + event.getPublicId());
        }
    }

    @KafkaListener(topics = "account-stream")
    public void receiveAccountStreamMessage(@Payload String message) throws JsonProcessingException {
        log.info("Message received : " + message);

        var mapper = new ObjectMapper();
        var event = mapper.readValue(message, AccountEvent.class);

        if (event.getEventType().equals("AccountUpdated")) {
            var user = userRepository.getByPublicId(event.getUserPublicId());
            var account = accountRepository.getByUserId(user.getId());

            account.setBalance(event.getBalance());
            accountRepository.save(account);
        }
    }
}

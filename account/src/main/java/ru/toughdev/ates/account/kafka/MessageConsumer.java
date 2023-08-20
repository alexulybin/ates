package ru.toughdev.ates.account.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.toughdev.ates.account.model.Account;
import ru.toughdev.ates.account.model.Payment;
import ru.toughdev.ates.account.model.Task;
import ru.toughdev.ates.account.model.User;
import ru.toughdev.ates.account.repository.AccountRepository;
import ru.toughdev.ates.account.repository.PaymentRepository;
import ru.toughdev.ates.account.repository.TaskRepository;
import ru.toughdev.ates.account.repository.UserRepository;
import ru.toughdev.ates.event.payment.PaymentEventV1;
import ru.toughdev.ates.event.task.TaskEventV1;
import ru.toughdev.ates.event.user.UserEventV1;

import java.time.LocalDateTime;
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
    private final MessageProducer messageProducer;

    @KafkaListener(topics = "user-stream")
    public void receiveUserStreamMessage(@Payload UserEventV1 event) throws JsonProcessingException {
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
    public void receiveTaskStreamMessage(@Payload TaskEventV1 event) throws JsonProcessingException {
        log.info("Message received : " + event);

        if (event.getEventType().equals("TaskCreated")) {
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
    }

    @KafkaListener(topics = "task-lifecycle")
    public void receiveTaskLifecycleMessage(@Payload TaskEventV1 event) throws JsonProcessingException {
        log.info("Message received : " + event);

        var user = userRepository.getByPublicId(UUID.fromString(event.getAssigneeId()));
        var task = taskRepository.getByPublicId(UUID.fromString(event.getPublicId()));

        switch (event.getEventType()) {
            case "TaskCompleted":
                var payment = savePayment(task.getId(), user.getId(), task.getReward(), "reward");

                sendPaymentEvent("payment-stream", "PaymentCreated", payment.getPublicId(),
                        task.getPublicId(), user.getPublicId(), payment.getAmount(), payment.getType());

                // на будущее, пока никем не консьюмится
                sendPaymentEvent("payment-lifecycle", "RewardPaid", payment.getPublicId(),
                        task.getPublicId(), user.getPublicId(), null, null);

                log.info("Reward paid " + payment.getTaskId());
                break;
            case "TaskAssigned":
            case "TaskReassigned":
                payment = savePayment(task.getId(), user.getId(), task.getFee(), "fee");

                sendPaymentEvent("payment-stream", "PaymentCreated", payment.getPublicId(),
                        task.getPublicId(), user.getPublicId(), payment.getAmount(), payment.getType());

                // на будущее, пока никем не консьюмится
                sendPaymentEvent("payment-lifecycle", "FeePaid", payment.getPublicId(),
                        task.getPublicId(), user.getPublicId(), null, null);

                log.info("Fee paid " + payment.getTaskId());
                break;
        }
    }

    private void sendPaymentEvent(String topic, String eventType, UUID publicId, UUID taskPublicId, UUID userPublicId,
                                  Long amount, String type)
            throws JsonProcessingException {

        var ldt = LocalDateTime.now();
        var datetime  = ldt.atZone(TimeZone.getTimeZone("UTC").toZoneId()).toInstant().toEpochMilli();

        var event = PaymentEventV1.newBuilder()
                .setEventType(eventType)
                .setPublicId(publicId.toString())
                .setTaskPublicId(taskPublicId.toString())
                .setUserPublicId(userPublicId.toString())
                .setAmount(amount)
                .setType(type)
                .setDateTime(datetime)
                .build();

        messageProducer.sendMessage(event, topic);
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
}

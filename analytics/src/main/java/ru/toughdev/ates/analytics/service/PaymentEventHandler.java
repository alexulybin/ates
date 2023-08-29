package ru.toughdev.ates.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.toughdev.ates.analytics.model.Payment;
import ru.toughdev.ates.analytics.repository.AccountRepository;
import ru.toughdev.ates.analytics.repository.PaymentRepository;
import ru.toughdev.ates.analytics.repository.TaskRepository;
import ru.toughdev.ates.analytics.repository.UserRepository;
import ru.toughdev.ates.event.payment.PaymentMadeEventV1;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PaymentRepository paymentRepository;

    public void handle(PaymentMadeEventV1 event) {
        var user = userRepository.getByPublicId(UUID.fromString(event.getUserPublicId()));
        var task = taskRepository.getByPublicId(UUID.fromString(event.getTaskPublicId()));

        var dateTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDateTime()),
                        TimeZone.getTimeZone("UTC").toZoneId());

        var paymentFee = Payment.builder()
                .taskId(task.getId())
                .userId(user.getId())
                .amount(event.getAmount())
                .type(event.getType())
                .dateTime(dateTime)
                .build();

        paymentRepository.save(paymentFee);
        log.info("PaymentCreated");
    }
}

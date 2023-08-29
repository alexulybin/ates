package ru.toughdev.ates.analytics.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.toughdev.ates.analytics.model.Account;
import ru.toughdev.ates.analytics.model.User;
import ru.toughdev.ates.analytics.repository.AccountRepository;
import ru.toughdev.ates.analytics.repository.UserRepository;
import ru.toughdev.ates.analytics.service.PaymentEventHandler;
import ru.toughdev.ates.analytics.service.TaskEventHandler;
import ru.toughdev.ates.event.account.AccountBalanceUpdatedEventV1;
import ru.toughdev.ates.event.payment.PaymentMadeEventV1;
import ru.toughdev.ates.event.task.TaskCompletedEventV1;
import ru.toughdev.ates.event.task.TaskCreatedEventV1;
import ru.toughdev.ates.event.task.TaskCreatedEventV2;
import ru.toughdev.ates.event.user.UserCreatedEventV1;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    private final TaskEventHandler taskEventHandler;
    private final PaymentEventHandler paymentEventHandler;

    @KafkaListener(topics = "user-stream")
    public void receiveUserStreamMessage(@Payload SpecificRecord record) {
        log.info("Message received : " + record);

        if (record instanceof UserCreatedEventV1) {
            var event = (UserCreatedEventV1) record;
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

        if (record instanceof TaskCreatedEventV1) {
            taskEventHandler.handle((TaskCreatedEventV1) record);
        }

        if (record instanceof TaskCreatedEventV2) {
            taskEventHandler.handle((TaskCreatedEventV2) record);
        }
    }

    @KafkaListener(topics = "task-lifecycle")
    public void receiveTaskLifecycleMessage(@Payload SpecificRecord record) {
        log.info("Message received : " + record);

        if (record instanceof TaskCompletedEventV1) {
            taskEventHandler.handle((TaskCompletedEventV1) record);
        }
    }

    @KafkaListener(topics = "payment-lifecycle")
    public void receivePaymentLifecycleMessage(@Payload SpecificRecord record) {
        log.info("Message received : " + record);

        if (record instanceof PaymentMadeEventV1) {
            paymentEventHandler.handle((PaymentMadeEventV1) record);
        }
    }

    @KafkaListener(topics = "account-stream")
    public void receiveAccountStreamMessage(@Payload SpecificRecord record) {
        log.info("Message received : " + record);

        if (record instanceof AccountBalanceUpdatedEventV1) {
            var event = (AccountBalanceUpdatedEventV1) record;
            var user = userRepository.getByPublicId(UUID.fromString(event.getUserPublicId()));
            var account = accountRepository.getByUserId(user.getId());

            account.setBalance(event.getBalance());
            accountRepository.save(account);
        }
    }
}

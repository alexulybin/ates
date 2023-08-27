package ru.toughdev.ates.account.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.toughdev.ates.account.model.Account;
import ru.toughdev.ates.account.model.User;
import ru.toughdev.ates.account.repository.AccountRepository;
import ru.toughdev.ates.account.repository.UserRepository;
import ru.toughdev.ates.account.service.TaskEventHandler;
import ru.toughdev.ates.event.task.TaskAddedEventV1;
import ru.toughdev.ates.event.task.TaskAddedEventV2;
import ru.toughdev.ates.event.task.TaskCompletedEventV1;
import ru.toughdev.ates.event.task.TaskCreatedEventV1;
import ru.toughdev.ates.event.task.TaskCreatedEventV2;
import ru.toughdev.ates.event.task.TaskReassignedEventV1;
import ru.toughdev.ates.event.user.UserCreatedEventV1;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    private final TaskEventHandler taskEventHandler;

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

        if (record instanceof TaskAddedEventV1) {
            taskEventHandler.handle((TaskAddedEventV1) record);
        }

        if (record instanceof TaskAddedEventV2) {
            taskEventHandler.handle((TaskAddedEventV2) record);
        }

        if (record instanceof TaskReassignedEventV1) {
            taskEventHandler.handle((TaskReassignedEventV1) record);
        }

        if (record instanceof TaskCompletedEventV1) {
            taskEventHandler.handle((TaskCompletedEventV1) record);
        }
    }
}

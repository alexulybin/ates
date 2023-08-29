package ru.toughdev.ates.tasktracker.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.toughdev.ates.event.user.UserCreatedEventV1;
import ru.toughdev.ates.tasktracker.model.User;
import ru.toughdev.ates.tasktracker.repository.UserRepository;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final UserRepository userRepository;

    @KafkaListener(topics = "user-stream")
    public void receive(@Payload SpecificRecord record) {
        log.info("Message received : " + record);

        if (record instanceof UserCreatedEventV1) {
            var event = (UserCreatedEventV1) record;
            var user = User.builder()
                    .publicId(UUID.fromString(event.getPublicId()))
                    .login(event.getLogin())
                    .email(event.getEmail())
                    .role(event.getRole())
                    .build();

            if (!userRepository.existsByPublicId(user.getPublicId())) {
                userRepository.save(user);
            }
        }
    }
}

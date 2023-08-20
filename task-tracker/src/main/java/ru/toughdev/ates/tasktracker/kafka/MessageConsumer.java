package ru.toughdev.ates.tasktracker.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.toughdev.ates.event.user.UserEventV1;
import ru.toughdev.ates.tasktracker.model.User;
import ru.toughdev.ates.tasktracker.repository.UserRepository;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final UserRepository userRepository;

    @KafkaListener(topics = "user-stream")
    public void receive(@Payload UserEventV1 event) throws JsonProcessingException {

        log.info("Message received : " + event);

        if (event.getEventType().equals("UserCreated")) {
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

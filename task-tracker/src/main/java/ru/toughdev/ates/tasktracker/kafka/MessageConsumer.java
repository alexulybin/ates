package ru.toughdev.ates.tasktracker.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.toughdev.ates.tasktracker.kafka.event.UserEvent;
import ru.toughdev.ates.tasktracker.model.User;
import ru.toughdev.ates.tasktracker.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final UserRepository userRepository;

    @KafkaListener(topics = "user-stream")
    public void receive(@Payload String message) throws JsonProcessingException {

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

            if (!userRepository.existsByPublicId(user.getPublicId())) {
                userRepository.save(user);
            }
        }
    }
}

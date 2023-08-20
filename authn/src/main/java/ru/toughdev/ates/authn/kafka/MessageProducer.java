package ru.toughdev.ates.authn.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(Object event, String topic) {
        kafkaTemplate.send(topic, event);
    }
}

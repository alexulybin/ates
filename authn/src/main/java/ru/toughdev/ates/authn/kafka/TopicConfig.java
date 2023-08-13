package ru.toughdev.ates.authn.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Первичное создание необходимых топиков
 */

@Configuration
public class TopicConfig {

    @Bean
    public NewTopic usersStreamTopic() {
        return new NewTopic("user-stream", 1, (short) 1);
    }
}

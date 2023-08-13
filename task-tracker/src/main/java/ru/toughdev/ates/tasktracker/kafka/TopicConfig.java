package ru.toughdev.ates.tasktracker.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Первичное создание необходимых топиков
 */

@Configuration
public class TopicConfig {

    @Bean
    public NewTopic tasksTopic() {
        return new NewTopic("task-lifecycle", 1, (short) 1);
    }

    @Bean
    public NewTopic tasksStreamTopic() {
        return new NewTopic("task-stream", 1, (short) 1);
    }
}

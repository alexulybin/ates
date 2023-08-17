package ru.toughdev.ates.account.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Первичное создание необходимых топиков
 */

@Configuration
public class TopicConfig {

    @Bean
    public NewTopic accountLifecycleTopic() {
        return new NewTopic("account-lifecycle", 1, (short) 1);
    }

    @Bean
    public NewTopic accountStreamTopic() {
        return new NewTopic("account-stream", 1, (short) 1);
    }

    @Bean
    public NewTopic paymentLifecycleTopic() {
        return new NewTopic("payment-lifecycle", 1, (short) 1);
    }
}

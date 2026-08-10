package com.jeysiva.payment;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Declares the saga topics (both services declare them; creation is idempotent). */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic paymentRequests() {
        return TopicBuilder.name("payment-requests").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic paymentResults() {
        return TopicBuilder.name("payment-results").partitions(1).replicas(1).build();
    }
}

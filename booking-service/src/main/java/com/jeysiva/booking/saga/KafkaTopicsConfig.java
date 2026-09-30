package com.jeysiva.booking.saga;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Auto-creates the saga topics at startup (single broker → 1 partition, 1 replica). */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic paymentRequests() {
        return TopicBuilder.name(SagaTopics.PAYMENT_REQUESTS).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic paymentResults() {
        return TopicBuilder.name(SagaTopics.PAYMENT_RESULTS).partitions(1).replicas(1).build();
    }

    // Where messages land once retries are exhausted or the failure is permanent. Declared up front so
    // the first dead letter does not depend on Kafka's auto-create being switched on.
    @Bean
    NewTopic paymentRequestsDlt() {
        return TopicBuilder.name(SagaTopics.PAYMENT_REQUESTS_DLT).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic paymentResultsDlt() {
        return TopicBuilder.name(SagaTopics.PAYMENT_RESULTS_DLT).partitions(1).replicas(1).build();
    }
}

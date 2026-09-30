package com.jeysiva.payment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * What happens when the payment listener throws: retry a temporary failure 3 times with growing gaps,
 * send a permanent one (or an exhausted retry) to "<topic>.DLT" so nothing is silently dropped.
 * See booking-service's KafkaErrorHandlerConfig for the longer explanation.
 */
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);

        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), backOff);

        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class,
                PaymentNotFoundException.class);

        return handler;
    }
}

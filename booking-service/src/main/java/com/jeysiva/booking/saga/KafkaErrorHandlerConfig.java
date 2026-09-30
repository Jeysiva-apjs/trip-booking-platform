package com.jeysiva.booking.saga;

import com.jeysiva.booking.common.NotFoundException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * What happens when a @KafkaListener throws.
 *
 * <p>Without this, Spring retries a few times and then just logs — the message is gone, and with it a
 * payment result that would have settled a trip. With it, every failure ends somewhere you can see:
 *
 * <ul>
 *   <li>temporary problem (database blipped) → retry 3 times with growing gaps; usually succeeds</li>
 *   <li>permanent problem (unreadable message, trip does not exist) → straight to the dead letter topic,
 *       because retrying will never help</li>
 *   <li>retries used up → also the dead letter topic</li>
 * </ul>
 *
 * <p>The dead letter topic is just another Kafka topic, named after the original with ".DLT" on the end.
 * Broken messages sit there so you can look at them, fix the cause, and replay them by hand.
 *
 * <p>Boot picks this bean up automatically — a single CommonErrorHandler bean is applied to every
 * listener container.
 */
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // Where a give-up message goes: same partition of "<original-topic>.DLT".
        DeadLetterPublishingRecoverer toDeadLetterTopic = new DeadLetterPublishingRecoverer(kafkaTemplate);

        // 3 retries, gap doubling each time (1s, 2s, 4s) so a brief outage has time to clear.
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);

        DefaultErrorHandler handler = new DefaultErrorHandler(toDeadLetterTopic, backOff);

        // These will fail identically every time — skip the retries and dead-letter immediately.
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class,
                NotFoundException.class);

        return handler;
    }
}

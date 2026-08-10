package com.jeysiva.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifies the Spring context wires up cleanly. Kafka listeners and the Eureka client are both
 * off so this needs no broker and no discovery-service running.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.listener.auto-startup=false",
        "eureka.client.enabled=false"
})
class BookingPlatformApplicationTests {

    @Test
    void contextLoads() {
    }
}

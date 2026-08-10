package com.jeysiva.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mock payment service on port 8081, with its own `payment` schema.
 * Run: ./mvnw -pl payment-service spring-boot:run
 *
 * <p>No {@code @EnableDiscoveryClient} needed — auto-configuration sees the Eureka client starter on the
 * classpath and registers this app at startup.
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

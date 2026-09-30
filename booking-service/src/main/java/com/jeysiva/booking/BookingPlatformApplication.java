package com.jeysiva.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @ComponentScan finds beans in this package and below, which is why this class sits at the root package.
// Eureka registration comes from auto-configuration — no @EnableDiscoveryClient needed.
// @EnableScheduling turns on the timer that OutboxPublisher runs on.
@SpringBootApplication
@EnableScheduling
public class BookingPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingPlatformApplication.class, args);
    }
}

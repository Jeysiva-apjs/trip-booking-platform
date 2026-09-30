package com.jeysiva.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The single entry point in front of booking-service and payment-service, so callers don't need to know
 * which port each service lives on. Routes target a Eureka service-id ({@code lb://booking-service})
 * rather than a fixed host:port.
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}

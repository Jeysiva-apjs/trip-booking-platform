package com.jeysiva.booking.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingPlatformOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini Booking Platform API")
                        .version("v1")
                        .description("A Trip = one flight seat + one hotel room, booked together as a single unit. "
                                + "Every endpoint is open — the platform has no authentication, and a trip's "
                                + "owner is whatever userId the caller sends."));
    }
}

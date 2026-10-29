package com.acme.roombooking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {
  @Bean
  OpenAPI workplaceApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Workplace Room Booking")
                .version("v1")
                .description(
                    "Meeting-room inventory, availability and personal bookings. "
                        + "Timestamps use ISO-8601 with an offset. Working hours use Europe/Bratislava. "
                        + "The supplied accounts are for local development only."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "basicAuth",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"))
                .addSecuritySchemes(
                    "csrfToken",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-CSRF-TOKEN")
                        .description(
                            "For writes: call GET /api/csrf first, keep its session cookie, "
                                + "then paste the returned token here.")))
        .addSecurityItem(new SecurityRequirement().addList("basicAuth").addList("csrfToken"));
  }
}

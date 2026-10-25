package com.eventhub.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EventHub Industrial Event API")
                        .version("1.0.0")
                        .description("API REST para recepção, processamento, deduplicação e consulta de eventos industriais.")
                        .contact(new Contact().name("EventHub Engineering Team")));
    }
}

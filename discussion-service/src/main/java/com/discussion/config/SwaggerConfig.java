package com.discussion.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures SpringDoc OpenAPI (Swagger UI) for the Discussion Service.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI discussionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Discussion Service API")
                .description("API for managing course discussions and Q&A")
                .version("v1.0"));
    }
}

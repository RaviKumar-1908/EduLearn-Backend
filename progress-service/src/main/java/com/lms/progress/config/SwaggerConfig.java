package com.lms.progress.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures SpringDoc OpenAPI (Swagger UI).
 * Access the UI at: http://localhost:8087/swagger-ui/index.html
 *
 * A global Bearer token security scheme is registered so that
 * protected endpoints can be tested directly from the Swagger UI.
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI progressServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LMS Progress & Certificate Service API")
                        .description("Tracks lesson-level learning activity and issues completion certificates")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("LMS Dev Team")
                                .email("dev@lms.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}

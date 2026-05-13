package com.lms.notification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SwaggerConfig – SpringDoc OpenAPI 3 configuration for the Notification-Service.
 *
 * <p>Accessible at:
 * <ul>
 *   <li>Swagger UI: {@code http://localhost:8086/swagger-ui.html}</li>
 *   <li>API Docs JSON: {@code http://localhost:8086/v3/api-docs}</li>
 * </ul>
 *
 * <p>JWT Bearer authentication is pre-configured in the UI so testers
 * can paste a token and hit secured endpoints directly.
 *
 * @author LMS Team
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LMS – Notification Service API")
                        .version("1.0.0")
                        .description("REST API for dispatching and managing in-app and email notifications in the LMS platform.")
                        .contact(new Contact()
                                .name("LMS Team")
                                .email("support@lms.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8000/notification-service")
                                    .description("Via API Gateway (port 8000)"),
                        new Server().url("http://localhost:8086")
                                    .description("Direct access (dev only)")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("BearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the JWT token obtained from the Auth-Service login endpoint")));
    }
}

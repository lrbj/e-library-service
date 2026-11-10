package com.elibrary.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the full API at /swagger-ui.html so every endpoint can be exercised
 * without curl. The X-User-Id header is declared as a security scheme so
 * the "Authorize" button fills it in for all calls.
 */
@Configuration
public class OpenApiConfig {

    private static final String USER_SCHEME = "X-User-Id";

    @Bean
    public OpenAPI eLibraryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Library Service API")
                        .version("v1")
                        .description("Browse, borrow and return digital titles. "
                                + "Set the X-User-Id header to act as a given user.")
                        .contact(new Contact().name("E-Library Service"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(USER_SCHEME))
                .schemaRequirement(USER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(USER_SCHEME)
                        .description("Any non-empty user identifier, e.g. alice"));
    }
}
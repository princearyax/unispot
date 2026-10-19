package com.prince.unispot.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


// this gonna register the API title, architectural description, and the global bearerAuth JWT security scheme so that the "Authorize" button appears in Swagger UI
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth"; //configure to let them know we using jwt and show corrsponding ui

    @Bean
    public OpenAPI unispotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UniSpot API — Hyperlocal Campus Discovery Backend")
                        .version("v1.0.0")
                        .description("""
                                Production-grade REST API built with Java 21 (Virtual Threads) and Spring Boot 4.
                                Key architectural showcases:
                                - Stateless JWT Access Tokens + Rotating HttpOnly Refresh Token Cookies
                                - Keyset (Cursor) Pagination backed by composite B-Tree indexes
                                - Optimistic Concurrency Control (@Version) with REQUIRES_NEW retry isolation
                                - Non-transactional Cloudinary multipart streaming with compensating rollback
                                """)
                        .contact(new Contact()
                                .name("Prince Arya")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT accessToken returned from /api/v1/auth/login or /register")));
    }
}
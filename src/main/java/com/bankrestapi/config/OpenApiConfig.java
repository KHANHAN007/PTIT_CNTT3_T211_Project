package com.bankrestapi.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI bankOpenApi() {
        String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("Rikkei Bank REST API").version("v1")
                        .description("Secure banking API with JWT, eKYC, ledger and maker-checker workflows"))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}

package com.mylog.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"api", "local"})
public class OpenApiConfig {

    @Bean
    OpenAPI myLogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyLog Backend API")
                        .version("v1")
                        .description("Backend API for the MyLog journaling and insight platform"))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

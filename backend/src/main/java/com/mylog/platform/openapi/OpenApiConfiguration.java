package com.mylog.platform.openapi;

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

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {
    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI mylogOpenApi() {
        var bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Short-lived mylog access token");

        return new OpenAPI()
                .info(new Info()
                        .title("mylog Backend API")
                        .version("v1")
                        .description("API cho nền tảng nhật ký thông minh mylog. Không phải dịch vụ chẩn đoán hoặc điều trị y tế.")
                        .contact(new Contact().name("mylog backend team"))
                        .license(new License().name("Private project")))
                .servers(List.of(new Server().url("/").description("Current environment")))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}

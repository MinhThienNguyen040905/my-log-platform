package com.mylog.platform.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "mylog")
public record MylogProperties(
        @NotBlank @Pattern(regexp = "api|worker|all") String appProfile,
        @Valid Web web,
        @Valid OpenApi openApi,
        @Valid Identity identity
) {
    public record Web(@NotEmpty List<@NotBlank String> allowedOrigins) {
    }

    public record OpenApi(boolean enabled) {
    }

    public record Identity(boolean enabled) {
    }
}

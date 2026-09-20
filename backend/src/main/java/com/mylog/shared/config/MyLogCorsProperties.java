package com.mylog.shared.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mylog.cors")
public record MyLogCorsProperties(List<String> allowedOrigins) {

    public MyLogCorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}

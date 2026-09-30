package com.mylog.platform.openapi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigurationTest {

    @Test
    void declaresApiIdentityServerAndBearerSecurity() {
        var openApi = new OpenApiConfiguration().mylogOpenApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("mylog Backend API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openApi.getServers()).extracting(server -> server.getUrl()).containsExactly("/");
        assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openApi.getSecurity().getFirst().containsKey("bearerAuth")).isTrue();
    }
}

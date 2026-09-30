package com.mylog;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		var image = DockerImageName.parse("pgvector/pgvector:pg17")
				.asCompatibleSubstituteFor("postgres");
		return new PostgreSQLContainer(image);
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:8-alpine")).withExposedPorts(6379);
	}

}

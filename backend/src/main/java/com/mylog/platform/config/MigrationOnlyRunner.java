package com.mylog.platform.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Exit after Flyway validation/migration has completed during context startup. */
@Component
@Profile("migrate")
final class MigrationOnlyRunner implements ApplicationRunner {
    private final ConfigurableApplicationContext context;

    MigrationOnlyRunner(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        SpringApplication.exit(context);
    }
}

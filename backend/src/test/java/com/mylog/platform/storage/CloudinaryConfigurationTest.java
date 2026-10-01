package com.mylog.platform.storage;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CloudinaryConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CloudinaryConfiguration.class);

    @Test
    void doesNotCreateClientWhenDisabled() {
        contextRunner
                .withPropertyValues("mylog.cloudinary.enabled=false", "mylog.cloudinary.folder=mylog")
                .run(context -> assertThat(context).doesNotHaveBean(Cloudinary.class));
    }

    @Test
    void createsSecureClientWhenEnabled() {
        contextRunner.withPropertyValues(
                        "mylog.cloudinary.enabled=true",
                        "mylog.cloudinary.cloud-name=test-cloud",
                        "mylog.cloudinary.api-key=test-key",
                        "mylog.cloudinary.api-secret=test-secret",
                        "mylog.cloudinary.folder=mylog")
                .run(context -> {
                    assertThat(context).hasSingleBean(Cloudinary.class);
                    assertThat(context.getBean(Cloudinary.class).config.secure).isTrue();
                });
    }

    @Test
    void failsFastWhenEnabledWithoutCredentials() {
        contextRunner
                .withPropertyValues("mylog.cloudinary.enabled=true", "mylog.cloudinary.folder=mylog")
                .run(context -> assertThat(context).hasFailed());
    }
}

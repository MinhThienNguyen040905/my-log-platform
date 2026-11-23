package com.mylog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.hibernate.autoconfigure.HibernateProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateSettings;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JpaConfigurationTest {
    @Test
    void applicationBindsSchemaValidationAndUtcToHibernate() {
        // Load only the checked-in YAML: no local .env, database, workers or provider calls.
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        var binder = new Binder(ConfigurationPropertySources.from(
                new PropertiesPropertySource("application", yaml.getObject())));
        var jpa = binder.bind("spring.jpa", JpaProperties.class).get();
        var hibernate = binder.bind("spring.jpa.hibernate", HibernateProperties.class).get();
        var settings = hibernate.determineHibernateProperties(jpa.getProperties(), new HibernateSettings());

        assertFalse(jpa.getOpenInView());
        assertEquals("validate", settings.get("hibernate.hbm2ddl.auto"));
        assertEquals("UTC", settings.get("hibernate.jdbc.time_zone"));
    }
}

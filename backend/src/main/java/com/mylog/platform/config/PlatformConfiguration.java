package com.mylog.platform.config;

import com.mylog.platform.id.IdGenerator;
import com.mylog.platform.id.UuidV7Generator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
class PlatformConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    SecureRandom secureRandom() {
        return new SecureRandom();
    }

    @Bean
    IdGenerator idGenerator(Clock clock, SecureRandom secureRandom) {
        return new UuidV7Generator(clock, secureRandom);
    }
}

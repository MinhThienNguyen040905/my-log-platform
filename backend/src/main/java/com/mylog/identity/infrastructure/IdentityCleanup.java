package com.mylog.identity.infrastructure;

import com.mylog.identity.application.IdentityStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
@ConditionalOnExpression("'${mylog.app-profile:all}' != 'api'")
class IdentityCleanup {
    private final IdentityStore store;
    private final Clock clock;
    IdentityCleanup(IdentityStore store, Clock clock) { this.store = store; this.clock = clock; }

    @Scheduled(cron = "0 15 3 * * *", zone = "UTC")
    void purge() { store.purgeExpired(clock.instant()); }
}

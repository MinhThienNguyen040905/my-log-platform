package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.application.JournalStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JournalCleanup {
    private final JournalStore store;
    private final Clock clock;

    JournalCleanup(JournalStore store, Clock clock) { this.store = store; this.clock = clock; }

    @Scheduled(cron = "0 35 3 * * *", zone = "UTC")
    @Transactional
    void purge() { store.purgeDeletedBefore(clock.instant().minus(Duration.ofDays(30))); }
}

package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.application.JournalAssetStorage;
import com.mylog.journal.application.JournalAssetStore;
import com.mylog.journal.application.JournalAssetData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
@ConditionalOnExpression("'${mylog.app-profile:all}' != 'api'")
public class OrphanJournalAssetCleanupJob {
    private final JournalAssetStore store;
    private final JournalAssetStorage storage;
    private final Clock clock;

    public OrphanJournalAssetCleanupJob(JournalAssetStore store, JournalAssetStorage storage, Clock clock) {
        this.store = store;
        this.storage = storage;
        this.clock = clock;
    }

    @Scheduled(cron = "${mylog.assets.cleanup-cron:0 20 4 * * *}", zone = "UTC")
    @Transactional
    public void cleanup() {
        Instant now = clock.instant();
        // 1. Purge PENDING_ATTACH orphans older than 72 hours
        Instant draftCutoff = now.minus(Duration.ofHours(72));
        List<JournalAssetData> orphans = store.findOrphansBefore(draftCutoff, 50);
        for (JournalAssetData orphan : orphans) {
            storage.delete(orphan.publicId, orphan.assetType, orphan.deliveryType);
            store.markDeleted(orphan.id, now);
        }

        // 2. Purge DELETED assets older than 30 days
        Instant retentionCutoff = now.minus(Duration.ofDays(30));
        store.purgeDeletedBefore(retentionCutoff);
    }
}


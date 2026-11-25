package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.application.AssetDeletionQueue;
import com.mylog.journal.application.JournalAssetStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Removes queued files from asset storage. Storage calls run outside any database transaction; each item is then
 * marked done or rescheduled with backoff (60 s × 2^attempt, at most 24 hours). After 10 failed attempts the item
 * keeps retrying daily with {@code ASSET_DELETE_STUCK} so operators can find it. Logs never contain file ids.
 */
@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
@ConditionalOnExpression("'${mylog.app-profile:all}' != 'api'")
public class AssetDeletionQueueJob {
    static final int BATCH = 20;
    static final int STUCK_AFTER = 10;
    private static final Logger log = LoggerFactory.getLogger(AssetDeletionQueueJob.class);
    private final AssetDeletionQueue queue;
    private final JournalAssetStorage storage;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public AssetDeletionQueueJob(AssetDeletionQueue queue, JournalAssetStorage storage,
                                 TransactionTemplate transactions, Clock clock) {
        this.queue = queue; this.storage = storage; this.transactions = transactions; this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${mylog.assets.deletion-queue-delay-ms:30000}")
    public void run() {
        List<AssetDeletionQueue.Item> items = transactions.execute(s -> queue.claim(clock.instant(), BATCH));
        if (items == null || items.isEmpty()) return;
        int failed = 0;
        for (AssetDeletionQueue.Item item : items) {
            try {
                storage.delete(item.publicId(), item.assetType(), item.deliveryType());
                transactions.executeWithoutResult(s -> queue.done(item.id()));
            } catch (RuntimeException e) {
                failed++;
                Instant retryAt = clock.instant().plusSeconds(Math.min(86400, 60L << Math.min(item.attempt(), 10)));
                String code = item.attempt() >= STUCK_AFTER ? "ASSET_DELETE_STUCK" : "ASSET_DELETE_RETRY";
                transactions.executeWithoutResult(s -> queue.retry(item.id(), retryAt, code));
            }
        }
        if (failed > 0) log.warn("Asset deletion queue: {} of {} files will be retried", failed, items.size());
    }
}

package com.mylog.journal.infrastructure.persistence;

import com.mylog.journal.application.AssetDeletionQueue;
import com.mylog.journal.application.JournalAssetStorage;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class AssetDeletionQueueJobTest {
    private static final Instant NOW = Instant.parse("2026-11-25T00:00:00Z");
    private final AssetDeletionQueue queue = mock(AssetDeletionQueue.class);
    private final JournalAssetStorage storage = mock(JournalAssetStorage.class);
    private final AssetDeletionQueueJob job = new AssetDeletionQueueJob(queue, storage,
            new TransactionTemplate(mock(PlatformTransactionManager.class)), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test void deletedFilesLeaveTheQueue() {
        UUID id = UUID.randomUUID();
        when(queue.claim(NOW, AssetDeletionQueueJob.BATCH))
                .thenReturn(List.of(new AssetDeletionQueue.Item(id, "mylog/u/a", "IMAGE", "AUTHENTICATED", 1)));
        job.run();
        verify(storage).delete("mylog/u/a", "IMAGE", "AUTHENTICATED");
        verify(queue).done(id);
        verify(queue, never()).retry(any(), any(), any());
    }

    @Test void storageFailureIsRetriedWithBackoffAndOneFailureDoesNotStopTheBatch() {
        UUID failing = UUID.randomUUID(), fine = UUID.randomUUID();
        when(queue.claim(NOW, AssetDeletionQueueJob.BATCH)).thenReturn(List.of(
                new AssetDeletionQueue.Item(failing, "mylog/u/bad", "IMAGE", "AUTHENTICATED", 3),
                new AssetDeletionQueue.Item(fine, "mylog/u/ok", "IMAGE", "AUTHENTICATED", 1)));
        doThrow(new IllegalStateException("synthetic outage")).when(storage).delete(eq("mylog/u/bad"), any(), any());
        job.run();
        verify(queue).retry(failing, NOW.plusSeconds(60L << 3), "ASSET_DELETE_RETRY");
        verify(queue).done(fine);
    }

    @Test void repeatedFailuresAreMarkedStuckAndBackoffStopsGrowing() {
        UUID id = UUID.randomUUID();
        when(queue.claim(NOW, AssetDeletionQueueJob.BATCH))
                .thenReturn(List.of(new AssetDeletionQueue.Item(id, "mylog/u/x", "IMAGE", "AUTHENTICATED", 12)));
        doThrow(new IllegalStateException("synthetic outage")).when(storage).delete(any(), any(), any());
        job.run();
        // 60 s × 2^10 is the largest step (about 17 hours), below the 24-hour cap.
        verify(queue).retry(id, NOW.plusSeconds(60L << 10), "ASSET_DELETE_STUCK");
    }

    @Test void emptyQueueDoesNothing() {
        when(queue.claim(any(), anyInt())).thenReturn(List.of());
        job.run();
        verifyNoInteractions(storage);
    }
}

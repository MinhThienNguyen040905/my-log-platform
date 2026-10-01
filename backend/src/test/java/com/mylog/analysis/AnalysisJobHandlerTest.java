package com.mylog.analysis;

import com.mylog.analysis.application.AnalysisJobHandler;
import com.mylog.analysis.application.AnalysisStore;
import com.mylog.analysis.application.JournalAnalyzer;
import com.mylog.journal.application.JournalAnalysisAccess;
import com.mylog.safety.application.SafetyAnalysisPermission;
import com.mylog.user.application.UserProfileUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalysisJobHandlerTest {
    private final JournalAnalysisAccess journal = mock(JournalAnalysisAccess.class);
    private final UserProfileUseCase users = mock(UserProfileUseCase.class);
    private final SafetyAnalysisPermission safety = mock(SafetyAnalysisPermission.class);
    private final JournalAnalyzer analyzer = mock(JournalAnalyzer.class);
    private final AnalysisStore store = mock(AnalysisStore.class);
    private final TransactionTemplate tx = mock(TransactionTemplate.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final UUID jobId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID entryId = UUID.randomUUID();
    private AnalysisJobHandler handler;

    @BeforeEach void setup() {
        when(tx.execute(any())).thenAnswer(call ->
                ((org.springframework.transaction.support.TransactionCallback<?>) call.getArgument(0)).doInTransaction(null));
        handler = new AnalysisJobHandler(journal, users, safety, analyzer, store, tx, clock);
    }

    @Test void noConsentMeansNoDecryptionOrProviderCall() {
        assertEquals(AnalysisJobHandler.Outcome.CONSENT_REQUIRED,
                handler.process(jobId, "worker", userId, entryId, 1));
        verify(journal).cancel(eq(userId), eq(entryId), eq(1), any());
        verify(journal, never()).load(any(), any(), anyInt());
        verifyNoInteractions(analyzer, store);
    }

    @Test void consentWithdrawnDuringProviderCallDoesNotStoreOutput() {
        when(users.isConsentGranted(userId, "AI_PROCESSING")).thenReturn(true, false);
        when(safety.permits(userId, entryId)).thenReturn(true);
        when(journal.load(userId, entryId, 1)).thenReturn(
                new JournalAnalysisAccess.Input(userId, entryId, 1, "Synthetic", "A calm day"));
        when(journal.start(eq(userId), eq(entryId), eq(1), any())).thenReturn(true);
        when(analyzer.analyze(anyString(), anyString())).thenReturn(result());
        when(store.ownsLease(eq(jobId), eq("worker"), any())).thenReturn(true);

        assertEquals(AnalysisJobHandler.Outcome.CONSENT_REQUIRED,
                handler.process(jobId, "worker", userId, entryId, 1));
        verify(store, never()).save(any(), any(), any(), anyInt(), any(), any(), anyLong(), anyBoolean(), any());
        verify(journal).cancel(eq(userId), eq(entryId), eq(1), any());
    }

    @Test void failedActivationMarksCompletedAnalysisStale() {
        UUID analysisId = UUID.randomUUID();
        when(users.isConsentGranted(userId, "AI_PROCESSING")).thenReturn(true);
        when(safety.permits(userId, entryId)).thenReturn(true);
        when(safety.policyVersion(userId, entryId)).thenReturn("approved-v1");
        when(journal.load(userId, entryId, 1)).thenReturn(
                new JournalAnalysisAccess.Input(userId, entryId, 1, "Synthetic", "A calm day"));
        when(journal.start(eq(userId), eq(entryId), eq(1), any())).thenReturn(true);
        when(analyzer.analyze(anyString(), anyString())).thenReturn(result());
        when(store.ownsLease(eq(jobId), eq("worker"), any())).thenReturn(true);
        when(store.save(eq(jobId), eq(userId), eq(entryId), eq(1), eq("approved-v1"),
                any(), anyLong(), eq(true), any())).thenReturn(analysisId);

        assertEquals(AnalysisJobHandler.Outcome.STALE,
                handler.process(jobId, "worker", userId, entryId, 1));
        verify(store).markStale(analysisId);
    }

    private static JournalAnalyzer.Result result() {
        return new JournalAnalyzer.Result("NEUTRAL", new BigDecimal("0.5"), List.of(), List.of(),
                "Bạn có thể suy ngẫm thêm.", "FAKE", "fake", "1", "v1", 0, 0, BigDecimal.ZERO);
    }
}

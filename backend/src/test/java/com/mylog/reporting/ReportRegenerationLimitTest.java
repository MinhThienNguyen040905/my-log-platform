package com.mylog.reporting;

import com.mylog.insight.application.DailyAggregateSource;
import com.mylog.insight.application.InsightService;
import com.mylog.insight.application.NarrativeRewriter;
import com.mylog.journal.application.JournalService;
import com.mylog.platform.web.ConflictException;
import com.mylog.reporting.application.ReportService;
import com.mylog.reporting.application.ReportStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/** MYLOG_REPORTS_WEEKLY_REGENERATE_LIMIT: true = once per local week, false = any time (local testing). */
class ReportRegenerationLimitTest {
    static final Clock WEDNESDAY = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneId.of("UTC"));
    final UUID user = UUID.randomUUID();
    final UUID report = UUID.randomUUID();

    private ReportService service(ReportStore store, boolean limit) {
        DailyAggregateSource metrics = mock(DailyAggregateSource.class);
        when(metrics.timezone(user)).thenReturn("Asia/Ho_Chi_Minh");
        return new ReportService(store, metrics, mock(InsightService.class), mock(NarrativeRewriter.class),
                mock(JournalService.class), WEDNESDAY, limit);
    }

    @Test void withoutTheLimitRegenerationIsAlwaysAvailable() {
        ReportStore store = mock(ReportStore.class);
        UUID next = UUID.randomUUID();
        when(store.regenerateOnce(eq(user), eq(report), isNull(), any())).thenReturn(next);
        var service = service(store, false);

        var allowance = service.regenerationAllowance(user);
        assertTrue(allowance.available());
        assertEquals(LocalDate.of(2026, 11, 11), allowance.availableFrom());
        assertEquals(next, service.regenerate(user, report));
        assertEquals(next, service.regenerate(user, report));
        verify(store, never()).regenerationsSince(any(), any());
        verify(store, times(2)).regenerateOnce(eq(user), eq(report), isNull(), any());
    }

    @Test void withTheLimitTheWeekStartsOnLocalMonday() {
        ReportStore store = mock(ReportStore.class);
        Instant mondayLocalMidnight = Instant.parse("2026-11-08T17:00:00Z"); // 2026-11-09 00:00 in Asia/Ho_Chi_Minh
        when(store.regenerationsSince(user, mondayLocalMidnight)).thenReturn(1);
        when(store.regenerateOnce(eq(user), eq(report), eq(mondayLocalMidnight), any())).thenReturn(null);
        var service = service(store, true);

        var allowance = service.regenerationAllowance(user);
        assertFalse(allowance.available());
        assertEquals(LocalDate.of(2026, 11, 16), allowance.availableFrom());
        assertThrows(ConflictException.class, () -> service.regenerate(user, report));
    }
}

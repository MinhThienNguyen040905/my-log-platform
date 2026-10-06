package com.mylog.analysis.application;

import com.mylog.journal.application.JournalAnalysisAccess;
import com.mylog.journal.application.JournalService;
import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.RateLimitExceededException;
import com.mylog.safety.application.SafetyAnalysisPermission;
import com.mylog.user.application.UserProfileUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class AnalysisRetryService {
    private final JournalService journal;
    private final JournalAnalysisAccess access;
    private final AnalysisRetryStore jobs;
    private final UserProfileUseCase users;
    private final SafetyAnalysisPermission safety;
    private final Clock clock;

    AnalysisRetryService(JournalService journal, JournalAnalysisAccess access, AnalysisRetryStore jobs,
                         UserProfileUseCase users, SafetyAnalysisPermission safety, Clock clock) {
        this.journal = journal; this.access = access; this.jobs = jobs;
        this.users = users; this.safety = safety; this.clock = clock;
    }

    @Transactional
    public String retry(UUID userId, UUID entryId) {
        var entry = journal.get(userId, entryId);
        if ("ANALYZED".equals(entry.analysisStatus())) return "ANALYZED";
        if ("PENDING".equals(entry.analysisStatus()) || "ANALYZING".equals(entry.analysisStatus()))
            return entry.analysisStatus();
        if (!users.isConsentGranted(userId, "AI_PROCESSING")) throw new ConflictException("Cần đồng ý xử lý AI.");
        if (!safety.permits(userId, entryId)) throw new ConflictException("Chưa đủ điều kiện phân tích an toàn.");
        AnalysisRetryStore.State state = jobs.requeue(userId, entryId, entry.contentVersion(), clock.instant());
        if (state == AnalysisRetryStore.State.TOO_SOON) throw new RateLimitExceededException();
        if (state == AnalysisRetryStore.State.MISSING) throw new ConflictException("Job phân tích chưa sẵn sàng.");
        if (state == AnalysisRetryStore.State.QUEUED)
            access.retry(userId, entryId, entry.contentVersion(), clock.instant());
        return "PENDING";
    }
}

package com.mylog.analysis.application;

import com.mylog.platform.web.ConflictException;
import com.mylog.platform.web.InvalidRequestException;
import com.mylog.journal.application.JournalAnalysisAccess;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
public class AdminJobService {
    private final AdminJobStore store;
    private final Clock clock;
    private final JournalAnalysisAccess journal;
    public AdminJobService(AdminJobStore store, JournalAnalysisAccess journal, Clock clock) {
        this.store=store; this.journal=journal; this.clock=clock;
    }
    @Transactional(readOnly = true)
    public List<AdminJobStore.Metadata> list(UUID before, int limit) {
        if (limit<1 || limit>100) throw new InvalidRequestException();
        return store.list(before,limit);
    }
    @Transactional
    public void retry(UUID actor, UUID id) {
        var target=store.retry(id,clock.instant()).orElseThrow(() -> new ConflictException("Job không thể retry."));
        if (!journal.retry(target.userId(),target.entryId(),target.contentVersion(),clock.instant()))
            throw new ConflictException("Journal không còn ở trạng thái retry.");
        store.audit(actor,id,clock.instant());
    }
}

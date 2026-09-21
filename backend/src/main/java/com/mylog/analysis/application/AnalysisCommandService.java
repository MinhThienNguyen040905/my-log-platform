package com.mylog.analysis.application;

import com.mylog.analysis.infrastructure.persistence.AnalysisJobRepository;
import com.mylog.shared.api.ApiErrorCodes;
import com.mylog.shared.exception.ConflictException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AnalysisCommandService {

    private final AnalysisQueryService query;
    private final AnalysisJobRepository jobs;

    public AnalysisCommandService(AnalysisQueryService query, AnalysisJobRepository jobs) {
        this.query = query;
        this.jobs = jobs;
    }

    public void retryAnalysis(UUID userId, UUID journalId) {
        AnalysisQueryService.JournalState journal = query.ownedJournal(userId, journalId);
        if (!"ANALYSIS_FAILED".equals(journal.status())) {
            throw new ConflictException(ApiErrorCodes.ANALYSIS_NOT_RETRYABLE, "Only a failed current analysis can be retried");
        }
        jobs.retry(userId, journalId, journal.version(), "ANALYSIS");
    }

    public void regenerateReflections(UUID userId, UUID journalId) {
        AnalysisQueryService.JournalState journal = query.ownedJournal(userId, journalId);
        if (!"ANALYZED".equals(journal.status())) {
            throw new ConflictException(ApiErrorCodes.ANALYSIS_NOT_READY, "Analysis must complete before regenerating reflections");
        }
        jobs.retry(userId, journalId, journal.version(), "REFLECTION");
    }
}

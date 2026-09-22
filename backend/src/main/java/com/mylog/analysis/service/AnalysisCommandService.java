package com.mylog.analysis.service;

import com.mylog.analysis.repository.AnalysisJobRepository;
import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.ConflictException;
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

    public void enqueueRequestedAnalysis(UUID userId, UUID journalId, long journalVersion) {
        jobs.enqueue(userId, journalId, journalVersion, "ANALYSIS");
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

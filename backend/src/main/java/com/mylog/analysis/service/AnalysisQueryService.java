package com.mylog.analysis.service;

import com.mylog.analysis.repository.AnalysisQueryRepository;
import com.mylog.analysis.repository.AnalysisQueryRepository.AnalysisRow;
import com.mylog.common.api.ApiErrorCodes;
import com.mylog.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisQueryService {

    private final AnalysisQueryRepository repository;

    public AnalysisQueryService(AnalysisQueryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AnalysisView get(UUID userId, UUID journalId) {
        JournalState journal = ownedJournal(userId, journalId);
        AnalysisView.Result result = repository.findCurrentAnalysis(journalId, journal.version())
                .map(row -> result(journalId, row))
                .orElse(null);
        AnalysisView.Safety safety = latestSafety(userId, journalId, journal.version(), result);
        return new AnalysisView(journalId, journal.version(), apiStatus(journal.status(), journalId, journal.version()), result, safety);
    }

    @Transactional(readOnly = true)
    public ReflectionView reflections(UUID userId, UUID journalId) {
        JournalState journal = ownedJournal(userId, journalId);
        UUID batch = repository.findLatestReflectionBatch(journalId, journal.version()).orElse(null);
        if (batch == null) {
            return new ReflectionView(journalId, journal.version(), null, List.of());
        }
        List<ReflectionView.Question> questions = repository.findReflectionQuestions(batch).stream()
                .map(row -> new ReflectionView.Question(row.id(), row.position(), row.question(), row.createdAt()))
                .toList();
        return new ReflectionView(journalId, journal.version(), batch, questions);
    }

    public JournalState ownedJournal(UUID userId, UUID journalId) {
        return repository.findOwnedJournal(userId, journalId)
                .map(row -> new JournalState(row.version(), row.status()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        ApiErrorCodes.JOURNAL_NOT_FOUND, "Journal entry does not exist"));
    }

    private AnalysisView.Result result(UUID journalId, AnalysisRow row) {
        List<AnalysisView.Emotion> emotions = repository.findEmotions(row.id()).stream()
                .map(item -> new AnalysisView.Emotion(
                        item.type(), item.originalScore(), item.effectiveScore(), item.correctedByUser()))
                .toList();
        List<AnalysisView.Topic> topics = repository.findActiveTopics(journalId).stream()
                .map(item -> new AnalysisView.Topic(item.name(), item.confidence(), item.source()))
                .toList();
        return new AnalysisView.Result(
                row.id(), row.sentiment(), row.riskLevel(), row.summary(), row.explanation(), emotions, topics,
                row.provider(), row.model(), row.promptVersion(), row.analyzedAt());
    }

    private AnalysisView.Safety latestSafety(
            UUID userId, UUID journalId, long version, AnalysisView.Result result) {
        AnalysisView.Safety recorded = repository.findLatestSafety(userId, journalId, version)
                .map(row -> new AnalysisView.Safety(row.riskLevel(), true, row.actionTaken()))
                .orElse(null);
        if (recorded != null) return recorded;
        String risk = result == null ? "NORMAL" : result.riskLevel();
        return new AnalysisView.Safety(risk, "HIGH".equals(risk) || "CRITICAL".equals(risk), "NONE");
    }

    private String apiStatus(String journalStatus, UUID journalId, long version) {
        if ("ANALYZED".equals(journalStatus)) return "COMPLETED";
        if ("ANALYSIS_FAILED".equals(journalStatus)) return "FAILED";
        if ("ANALYSIS_OUTDATED".equals(journalStatus)) return "OUTDATED";
        return repository.findAnalysisJobStatus(journalId, version)
                .filter("PROCESSING"::equals)
                .map(ignored -> "PROCESSING")
                .orElse("PENDING");
    }

    public record JournalState(long version, String status) {}
}

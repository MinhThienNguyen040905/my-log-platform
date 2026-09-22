package com.mylog.analysis.service;

import com.mylog.analysis.config.AiProperties;
import com.mylog.analysis.entity.AnalysisJob;
import com.mylog.analysis.entity.SafetyDecision;
import com.mylog.analysis.provider.AiAnalysisInput;
import com.mylog.analysis.provider.AiAnalysisOutput;
import com.mylog.analysis.provider.AiGateway;
import com.mylog.analysis.provider.AiProviderException;
import com.mylog.analysis.repository.AnalysisJobRepository;
import com.mylog.analysis.repository.AnalysisJobRepository.JournalInput;
import com.mylog.analysis.repository.AnalysisResultRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnalysisWorker {

    private final AnalysisJobRepository jobs;
    private final DeterministicSafetyPolicy safetyPolicy;
    private final AiGateway gateway;
    private final AiOutputValidator validator;
    private final AnalysisResultRepository resultStore;
    private final AiProperties properties;
    private final MeterRegistry metrics;

    public AnalysisWorker(
            AnalysisJobRepository jobs,
            DeterministicSafetyPolicy safetyPolicy,
            AiGateway gateway,
            AiOutputValidator validator,
            AnalysisResultRepository resultStore,
            AiProperties properties,
            MeterRegistry metrics) {
        this.jobs = jobs;
        this.safetyPolicy = safetyPolicy;
        this.gateway = gateway;
        this.validator = validator;
        this.resultStore = resultStore;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${mylog.ai.poll-interval:1s}")
    public void scheduledRun() {
        if (properties.schedulingEnabled()) {
            processAvailable();
        }
    }

    public int processAvailable() {
        List<AnalysisJob> claimed = jobs.claimBatch();
        claimed.forEach(this::process);
        return claimed.size();
    }

    private void process(AnalysisJob job) {
        JournalInput journal = jobs.loadCurrentInput(job).orElse(null);
        if (journal == null) {
            jobs.markObsolete(job.id());
            return;
        }
        SafetyDecision safety = safetyPolicy.evaluate(journal.content());
        if (safety.blocksNormalResponse() && "ANALYSIS".equals(job.jobType())) {
            AiAnalysisOutput blocked = safetyOnlyOutput(safety);
            resultStore.saveAnalysis(job, blocked, safety, false);
            metrics.counter("safety.events", "risk", safety.riskLevel()).increment();
            return;
        }
        long started = System.nanoTime();
        try {
            AiAnalysisOutput output = validator.validate(gateway.analyze(new AiAnalysisInput(
                    job.journalId(), job.journalVersion(), journal.content(), journal.moodScore(),
                    journal.stressScore(), journal.energyScore(), properties.maxReflections())));
            output = safetyGuard(output);
            boolean saved = "REFLECTION".equals(job.jobType())
                    ? resultStore.saveReflectionsOnly(job, output)
                    : resultStore.saveAnalysis(job, output, safety, true);
            if (saved) {
                metrics.counter("analysis.jobs", "outcome", "completed", "type", job.jobType()).increment();
            } else {
                metrics.counter("analysis.jobs", "outcome", "obsolete", "type", job.jobType()).increment();
            }
        } catch (AiProviderException exception) {
            long latency = (System.nanoTime() - started) / 1_000_000;
            resultStore.recordFailure(job, exception.code(), latency);
            jobs.markFailure(job, exception.code(), exception.retryable());
            metrics.counter("analysis.jobs", "outcome", exception.retryable() ? "retry" : "failed", "type", job.jobType()).increment();
        }
    }

    private AiAnalysisOutput safetyOnlyOutput(SafetyDecision safety) {
        String summary = "Your safety may need immediate attention. Please use the support options shown by the app.";
        return new AiAnalysisOutput(
                properties.schemaVersion(), "NEGATIVE", safety.riskLevel(), List.of(), List.of(),
                summary, null, List.of(), "rules", "deterministic-safety-v1", properties.promptVersion(),
                0, 0, 0);
    }

    private AiAnalysisOutput safetyGuard(AiAnalysisOutput output) {
        if (!"HIGH".equals(output.riskLevel()) && !"CRITICAL".equals(output.riskLevel())) {
            return output;
        }
        return new AiAnalysisOutput(
                output.schemaVersion(), output.sentiment(), output.riskLevel(), output.emotions(), output.topics(),
                "Your safety may need immediate attention. Please use the support options shown by the app.",
                null, List.of(), output.provider(), output.model(), output.promptVersion(),
                output.inputTokens(), output.outputTokens(), output.latencyMs());
    }
}

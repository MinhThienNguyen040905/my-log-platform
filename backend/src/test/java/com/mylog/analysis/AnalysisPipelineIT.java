package com.mylog.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mylog.analysis.service.AnalysisView;
import com.mylog.analysis.service.CorrectionCommand;
import com.mylog.analysis.service.CorrectionCommand.EmotionCorrection;
import com.mylog.analysis.service.CorrectionCommand.Operation;
import com.mylog.analysis.service.CorrectionCommand.TopicCorrection;
import com.mylog.analysis.service.AnalysisCommandService;
import com.mylog.analysis.entity.AnalysisJob;
import com.mylog.analysis.service.AnalysisQueryService;
import com.mylog.analysis.service.AnalysisWorker;
import com.mylog.analysis.service.CorrectionService;
import com.mylog.analysis.port.AiAnalysisInput;
import com.mylog.analysis.port.AiAnalysisOutput;
import com.mylog.analysis.port.AiAnalysisPort;
import com.mylog.analysis.port.AiProviderException;
import com.mylog.analysis.repository.AnalysisJobRepository;
import com.mylog.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.main.web-application-type=servlet",
    "mylog.ai.circuit-failure-threshold=100"
})
@AutoConfigureMockMvc
@ActiveProfiles({"api", "integration-test"})
@Import(AnalysisPipelineIT.TestBeans.class)
class AnalysisPipelineIT extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private AnalysisJobRepository jobs;
    @Autowired private AnalysisWorker worker;
    @Autowired private SwitchableProvider provider;
    @Autowired private AnalysisQueryService query;
    @Autowired private CorrectionService corrections;
    @Autowired private AnalysisCommandService commands;
    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void cleanState() {
        provider.mode.set(Mode.SUCCESS);
        provider.calls.set(0);
        jdbc.execute("TRUNCATE TABLE processed_messages, outbox_events");
        jdbc.execute("TRUNCATE TABLE users CASCADE");
    }

    @Test
    void successfulProviderResultIsValidatedPersistedAndExposed() throws Exception {
        Fixture fixture = fixture("A calm private entry", 7);

        jobs.enqueue(fixture.userId(), fixture.journalId(), 1, "ANALYSIS");
        assertThat(worker.processAvailable()).isEqualTo(1);

        AnalysisView response = query.get(fixture.userId(), fixture.journalId());
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.result().sentiment()).isEqualTo("POSITIVE");
        assertThat(response.result().emotions()).extracting(AnalysisView.Emotion::effectiveScore)
                .containsExactly(new BigDecimal("0.8000"));
        assertThat(query.reflections(fixture.userId(), fixture.journalId()).questions()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_usage_records", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events WHERE event_type = 'journal.analysis.completed'
                """, Integer.class)).isEqualTo(1);

        mockMvc.perform(get("/api/v1/journals/{id}/analysis", fixture.journalId())
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.topics[0].name").value("work"));
    }

    @Test
    void timeout429And5xxAreRetriedWithoutLosingJournal() {
        assertTransientFailure(Mode.TIMEOUT, "AI_TIMEOUT");
        assertTransientFailure(Mode.RATE_LIMIT, "AI_HTTP_429");
        assertTransientFailure(Mode.SERVER_ERROR, "AI_HTTP_503");
    }

    @Test
    void malformedOutputFailsWithoutInfiniteRetry() {
        Fixture fixture = fixture("Malformed response case", 5);
        provider.mode.set(Mode.MALFORMED);
        jobs.enqueue(fixture.userId(), fixture.journalId(), 1, "ANALYSIS");

        worker.processAvailable();

        assertThat(jobStatus(fixture.journalId())).isEqualTo("FAILED");
        assertThat(journalStatus(fixture.journalId())).isEqualTo("ANALYSIS_FAILED");
        assertThat(jdbc.queryForObject(
                "SELECT last_error_code FROM analysis_jobs WHERE journal_entry_id = ?",
                String.class, fixture.journalId())).isEqualTo("AI_INVALID_OUTPUT");

        provider.mode.set(Mode.SUCCESS);
        commands.retryAnalysis(fixture.userId(), fixture.journalId());
        worker.processAvailable();
        assertThat(query.get(fixture.userId(), fixture.journalId()).status()).isEqualTo("COMPLETED");
    }

    @Test
    void staleAnalysisCannotOverwriteANewerJournalVersion() {
        Fixture fixture = fixture("Version one", 5);
        provider.mode.set(Mode.MAKE_STALE);
        provider.staleJournal.set(fixture.journalId());
        jobs.enqueue(fixture.userId(), fixture.journalId(), 1, "ANALYSIS");

        worker.processAvailable();

        assertThat(jobStatus(fixture.journalId())).isEqualTo("OBSOLETE");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM journal_analyses WHERE journal_entry_id = ?",
                Integer.class, fixture.journalId())).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT journal_version FROM journal_entries WHERE id = ?",
                Long.class, fixture.journalId())).isEqualTo(2L);
    }

    @Test
    void criticalRuleBlocksProviderAndNormalCoaching() {
        Fixture fixture = fixture("I want to die and end my life", 1);
        jobs.enqueue(fixture.userId(), fixture.journalId(), 1, "ANALYSIS");

        worker.processAvailable();

        AnalysisView response = query.get(fixture.userId(), fixture.journalId());
        assertThat(provider.calls).hasValue(0);
        assertThat(response.result().riskLevel()).isEqualTo("CRITICAL");
        assertThat(response.result().explanation()).isNull();
        assertThat(response.safety().blocksNormalResponse()).isTrue();
        assertThat(response.safety().actionTaken()).isEqualTo("SHOW_CRISIS_SUPPORT");
        assertThat(query.reflections(fixture.userId(), fixture.journalId()).questions()).isEmpty();
        String safetyRow = jdbc.queryForObject(
                "SELECT row_to_json(s)::text FROM safety_events s WHERE journal_entry_id = ?",
                String.class, fixture.journalId());
        assertThat(safetyRow).doesNotContain("want to die", "end my life");
    }

    @Test
    void providerDetectedHighRiskIsSanitizedBeforePersistence() {
        Fixture fixture = fixture("A phrase not covered by deterministic rules", 2);
        provider.mode.set(Mode.AI_HIGH);
        jobs.enqueue(fixture.userId(), fixture.journalId(), 1, "ANALYSIS");

        worker.processAvailable();

        AnalysisView response = query.get(fixture.userId(), fixture.journalId());
        assertThat(response.result().riskLevel()).isEqualTo("HIGH");
        assertThat(response.result().summary()).doesNotContain("Take a walk");
        assertThat(response.result().explanation()).isNull();
        assertThat(query.reflections(fixture.userId(), fixture.journalId()).questions()).isEmpty();
        assertThat(response.safety().actionTaken()).isEqualTo("SHOW_URGENT_SUPPORT");
    }

    @Test
    void correctionPreservesOriginalAndChangesEffectiveValuesWithAudit() {
        Fixture fixture = fixture("Correction case", 7);
        jobs.enqueue(fixture.userId(), fixture.journalId(), 1, "ANALYSIS");
        worker.processAvailable();

        corrections.correct(fixture.userId(), fixture.journalId(), new CorrectionCommand(
                List.of(new EmotionCorrection("JOY", Operation.UPDATE, new BigDecimal("0.2000"))),
                List.of(
                        new TopicCorrection("work", Operation.REMOVE, null),
                        new TopicCorrection("family", Operation.ADD, new BigDecimal("0.9000")))));

        AnalysisView response = query.get(fixture.userId(), fixture.journalId());
        AnalysisView.Emotion joy = response.result().emotions().getFirst();
        assertThat(joy.originalScore()).isEqualByComparingTo("0.8000");
        assertThat(joy.effectiveScore()).isEqualByComparingTo("0.2000");
        assertThat(joy.corrected()).isTrue();
        assertThat(response.result().topics()).extracting(AnalysisView.Topic::name).containsExactly("family");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM journal_corrections", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM outbox_events WHERE event_type = 'journal.corrected'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void userCannotReadAnotherUsersAnalysis() throws Exception {
        Fixture owner = fixture("Owned entry", 7);
        Fixture other = fixture("Other entry", 7);
        jobs.enqueue(owner.userId(), owner.journalId(), 1, "ANALYSIS");
        worker.processAvailable();

        mockMvc.perform(get("/api/v1/journals/{id}/analysis", owner.journalId())
                        .with(jwt().jwt(token -> token.subject(other.userId().toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOURNAL_NOT_FOUND"));
    }

    @Test
    void reflectionCanBeRegeneratedAsynchronously() {
        Fixture fixture = fixture("Reflection regeneration", 7);
        jobs.enqueue(fixture.userId(), fixture.journalId(), 1, "ANALYSIS");
        worker.processAvailable();
        UUID firstBatch = query.reflections(fixture.userId(), fixture.journalId()).generationBatchId();

        commands.regenerateReflections(fixture.userId(), fixture.journalId());
        worker.processAvailable();

        UUID latestBatch = query.reflections(fixture.userId(), fixture.journalId()).generationBatchId();
        assertThat(latestBatch).isNotEqualTo(firstBatch);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM reflection_questions WHERE journal_entry_id = ?",
                Integer.class, fixture.journalId())).isEqualTo(2);
    }

    @Test
    void concurrentWorkersDoNotClaimTheSameAnalysisJob() throws Exception {
        for (int index = 0; index < 20; index++) {
            Fixture fixture = fixture("Concurrent " + index, 5);
            jobs.enqueue(fixture.userId(), fixture.journalId(), 1, "ANALYSIS");
        }
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<List<AnalysisJob>>> futures = executor.invokeAll(List.of(
                    (Callable<List<AnalysisJob>>) jobs::claimBatch,
                    (Callable<List<AnalysisJob>>) jobs::claimBatch));
            List<AnalysisJob> first = futures.get(0).get();
            List<AnalysisJob> second = futures.get(1).get();
            Set<UUID> ids = new HashSet<>();
            first.forEach(job -> ids.add(job.id()));
            second.forEach(job -> ids.add(job.id()));
            assertThat(first).hasSize(10);
            assertThat(second).hasSize(10);
            assertThat(ids).hasSize(20);
        }
    }

    private void assertTransientFailure(Mode mode, String expectedCode) {
        Fixture fixture = fixture(mode.name(), 5);
        provider.mode.set(mode);
        jobs.enqueue(fixture.userId(), fixture.journalId(), 1, "ANALYSIS");
        worker.processAvailable();
        assertThat(jobStatus(fixture.journalId())).isEqualTo("RETRY_WAIT");
        assertThat(journalStatus(fixture.journalId())).isEqualTo("ANALYZING");
        assertThat(jdbc.queryForObject(
                "SELECT last_error_code FROM analysis_jobs WHERE journal_entry_id = ?",
                String.class, fixture.journalId())).isEqualTo(expectedCode);
        provider.mode.set(Mode.SUCCESS);
    }

    private Fixture fixture(String content, int mood) {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-21T08:00:00Z");
        jdbc.update("""
                INSERT INTO users (
                    id, email, email_normalized, password_hash, display_name, plan, status, created_at, updated_at, version)
                VALUES (?, ?, ?, 'hash', 'Test', 'FREE', 'ACTIVE', ?, ?, 0)
                """, userId, userId + "@example.com", userId + "@example.com", Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                INSERT INTO journal_entries (
                    id, user_id, content_text, content_format, mood_score, status, journal_version,
                    occurred_at, entry_date, timezone_at_entry, is_favorite, created_at, updated_at, version)
                VALUES (?, ?, ?, 'PLAIN_TEXT', ?, 'SAVED', 1, ?, DATE '2026-09-21', 'Asia/Ho_Chi_Minh', FALSE, ?, ?, 0)
                """, journalId, userId, content, mood, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        return new Fixture(userId, journalId);
    }

    private String jobStatus(UUID journalId) {
        return jdbc.queryForObject("SELECT status FROM analysis_jobs WHERE journal_entry_id = ?", String.class, journalId);
    }

    private String journalStatus(UUID journalId) {
        return jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?", String.class, journalId);
    }

    record Fixture(UUID userId, UUID journalId) {}
    enum Mode { SUCCESS, TIMEOUT, RATE_LIMIT, SERVER_ERROR, MALFORMED, MAKE_STALE, AI_HIGH }

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        SwitchableProvider switchableProvider(JdbcTemplate jdbc) {
            return new SwitchableProvider(jdbc);
        }
    }

    static class SwitchableProvider implements AiAnalysisPort {
        final JdbcTemplate jdbc;
        final AtomicReference<Mode> mode = new AtomicReference<>(Mode.SUCCESS);
        final AtomicReference<UUID> staleJournal = new AtomicReference<>();
        final AtomicInteger calls = new AtomicInteger();

        SwitchableProvider(JdbcTemplate jdbc) { this.jdbc = jdbc; }

        @Override
        public AiAnalysisOutput analyze(AiAnalysisInput input) {
            calls.incrementAndGet();
            switch (mode.get()) {
                case TIMEOUT -> throw new AiProviderException("AI_TIMEOUT", "timeout", true);
                case RATE_LIMIT -> throw new AiProviderException("AI_HTTP_429", "rate limited", true);
                case SERVER_ERROR -> throw new AiProviderException("AI_HTTP_503", "unavailable", true);
                case MAKE_STALE -> jdbc.update(
                        "UPDATE journal_entries SET journal_version = 2, status = 'ANALYSIS_OUTDATED' WHERE id = ?",
                        staleJournal.get());
                default -> { }
            }
            String schema = mode.get() == Mode.MALFORMED ? "wrong" : "1.0";
            String risk = mode.get() == Mode.AI_HIGH ? "HIGH" : "NORMAL";
            return new AiAnalysisOutput(
                    schema, "POSITIVE", risk,
                    List.of(new AiAnalysisOutput.Emotion("JOY", new BigDecimal("0.8000"))),
                    List.of(new AiAnalysisOutput.Topic("work", new BigDecimal("0.7000"))),
                    mode.get() == Mode.AI_HIGH ? "Take a walk and relax" : "Summary",
                    "Non-clinical explanation", List.of("What helped today?"),
                    "test", "test-model", "analysis-v1", 10, 5, 12);
        }
    }
}

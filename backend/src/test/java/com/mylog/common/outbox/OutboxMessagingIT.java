package com.mylog.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mylog.common.messaging.IdempotentMessageConsumer;
import com.mylog.common.messaging.MessagingTopology;
import com.mylog.common.outbox.MessageEnvelope;
import com.mylog.common.outbox.OutboxEvent;
import com.mylog.common.outbox.OutboxPublisher;
import com.mylog.common.outbox.OutboxRepository;
import com.mylog.common.outbox.OutboxTransport;
import com.mylog.common.outbox.OutboxWriter;
import com.mylog.common.outbox.RabbitOutboxTransport;
import com.mylog.support.AbstractIntegrationTest;
import com.rabbitmq.client.Channel;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.main.web-application-type=servlet")
@AutoConfigureMockMvc
@ActiveProfiles({"api", "integration-test"})
@Import(OutboxMessagingIT.TestBeans.class)
class OutboxMessagingIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "StrongPass123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private OutboxWriter outboxWriter;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private OutboxPublisher publisher;
    @Autowired private SwitchableTransport transport;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private RabbitAdmin rabbitAdmin;
    @Autowired private IdempotentMessageConsumer messageConsumer;
    @Autowired private AtomicProbe atomicProbe;

    @BeforeEach
    void cleanState() {
        transport.failPublish = false;
        transport.failDeadLetter = false;
        jdbcTemplate.execute("TRUNCATE TABLE processed_messages, outbox_events");
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        rabbitAdmin.purgeQueue(MessagingTopology.JOURNAL_EVENTS_QUEUE, true);
        rabbitAdmin.purgeQueue(MessagingTopology.ANALYSIS_QUEUE, true);
        rabbitAdmin.purgeQueue(MessagingTopology.DEAD_LETTER_QUEUE, true);
    }

    @Test
    void journalAndOutboxCommitTogetherAndPayloadContainsNoContent() throws Exception {
        String token = register("atomic@example.com");
        String sensitiveContent = "private journal content must not enter the event";

        mockMvc.perform(post("/api/v1/journals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(journalJson(sensitiveContent)))
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM journal_entries", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
                .isEqualTo(2);
        List<String> payloads = jdbcTemplate.queryForList(
                "SELECT payload::text FROM outbox_events", String.class);
        assertThat(payloads).allSatisfy(payload -> {
            assertThat(payload).doesNotContain(sensitiveContent);
            assertThat(payload).contains("journalId", "journalVersion", "userId");
        });

        UUID userId = jdbcTemplate.queryForObject("SELECT id FROM users", UUID.class);
        assertThatThrownBy(() -> atomicProbe.insertJournalThenFailOutbox(userId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM journal_entries", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void brokerFailureRetainsEventAndSuccessfulRetryPublishesWithConfirm() {
        UUID eventId = append("journal.created");
        transport.failPublish = true;

        assertThat(publisher.publishAvailable()).isEqualTo(1);
        assertThat(outboxStatus(eventId)).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT attempt_count FROM outbox_events WHERE id = ?", Integer.class, eventId))
                .isEqualTo(1);

        transport.failPublish = false;
        makeRetryEligible(eventId);
        assertThat(publisher.publishAvailable()).isEqualTo(1);
        assertThat(outboxStatus(eventId)).isEqualTo("PUBLISHED");
        Message delivered = rabbitTemplate.receive(MessagingTopology.JOURNAL_EVENTS_QUEUE, 2_000);
        assertThat(delivered).isNotNull();
        assertThat(delivered.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());
    }

    @Test
    void expiredPublishingLeaseIsRecoveredAfterPublisherRestart() {
        UUID eventId = append("journal.updated");
        List<OutboxEvent> claimed = outboxRepository.claimBatch(
                1, 3, Instant.now(), Instant.now().plusSeconds(30));
        assertThat(claimed).extracting(OutboxEvent::id).containsExactly(eventId);
        assertThat(outboxStatus(eventId)).isEqualTo("PUBLISHING");

        makeRetryEligible(eventId);
        assertThat(publisher.publishAvailable()).isEqualTo(1);
        assertThat(outboxStatus(eventId)).isEqualTo("PUBLISHED");
    }

    @Test
    void poisonEventMovesToDeadLetterQueueAfterMaximumAttempts() {
        UUID eventId = append("journal.created");
        transport.failPublish = true;

        for (int attempt = 0; attempt < 3; attempt++) {
            makeRetryEligible(eventId);
            publisher.publishAvailable();
        }

        assertThat(outboxStatus(eventId)).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT attempt_count FROM outbox_events WHERE id = ?", Integer.class, eventId))
                .isEqualTo(3);
        Message deadLetter = rabbitTemplate.receive(MessagingTopology.DEAD_LETTER_QUEUE, 2_000);
        assertThat(deadLetter).isNotNull();
        assertThat(deadLetter.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());
    }

    @Test
    void concurrentWorkersNeverClaimTheSameEvent() throws Exception {
        Set<UUID> expected = new HashSet<>();
        for (int index = 0; index < 20; index++) {
            expected.add(append("journal.updated"));
        }
        CountDownLatch start = new CountDownLatch(1);
        Callable<List<OutboxEvent>> claim = () -> {
            start.await();
            Instant now = Instant.now();
            return outboxRepository.claimBatch(10, 3, now, now.plusSeconds(30));
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<List<OutboxEvent>> first = executor.submit(claim);
            Future<List<OutboxEvent>> second = executor.submit(claim);
            start.countDown();
            Set<UUID> firstIds = ids(first.get());
            Set<UUID> secondIds = ids(second.get());
            assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
            Set<UUID> combined = new HashSet<>(firstIds);
            combined.addAll(secondIds);
            assertThat(combined).isEqualTo(expected);
        }
    }

    @Test
    void duplicateDeliveryRunsBusinessActionOnceAndAcknowledgesBoth() throws Exception {
        UUID messageId = UUID.randomUUID();
        MessageEnvelope envelope = new MessageEnvelope(
                messageId,
                "journal.created",
                1,
                Instant.now(),
                UUID.randomUUID(),
                Map.of("journalVersion", 1));
        Channel channel = mock(Channel.class);
        AtomicInteger executions = new AtomicInteger();

        messageConsumer.consume("test-consumer", envelope, 10L, channel, ignored -> executions.incrementAndGet());
        messageConsumer.consume("test-consumer", envelope, 11L, channel, ignored -> executions.incrementAndGet());

        assertThat(executions).hasValue(1);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM processed_messages WHERE consumer_name = 'test-consumer'",
                        Integer.class))
                .isEqualTo(1);
        verify(channel).basicAck(10L, false);
        verify(channel).basicAck(11L, false);
        verify(channel, times(0)).basicNack(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    private UUID append(String eventType) {
        return outboxWriter.append(
                "JOURNAL",
                UUID.randomUUID(),
                eventType,
                1,
                Map.of("journalVersion", 1));
    }

    private void makeRetryEligible(UUID eventId) {
        jdbcTemplate.update(
                "UPDATE outbox_events SET next_attempt_at = now() - interval '1 second' WHERE id = ?",
                eventId);
    }

    private String outboxStatus(UUID eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM outbox_events WHERE id = ?", String.class, eventId);
    }

    private Set<UUID> ids(List<OutboxEvent> events) {
        Set<UUID> ids = new HashSet<>();
        events.forEach(event -> ids.add(event.id()));
        return ids;
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"Outbox User",
                                 "acceptTerms":true,"acceptPrivacy":true}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return response.get("accessToken").asText();
    }

    private String journalJson(String content) {
        return """
                {"contentText":"%s","contentFormat":"PLAIN_TEXT","moodScore":7,
                 "occurredAt":"2026-09-21T00:00:00Z","timezoneAtEntry":"UTC","favorite":false}
                """.formatted(content);
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        @Primary
        SwitchableTransport switchableTransport(RabbitOutboxTransport delegate) {
            return new SwitchableTransport(delegate);
        }

        @Bean
        AtomicProbe atomicProbe(JdbcTemplate jdbcTemplate, OutboxWriter writer, Clock clock) {
            return new AtomicProbe(jdbcTemplate, writer, clock);
        }
    }

    static final class SwitchableTransport implements OutboxTransport {
        private final RabbitOutboxTransport delegate;
        volatile boolean failPublish;
        volatile boolean failDeadLetter;

        SwitchableTransport(RabbitOutboxTransport delegate) {
            this.delegate = delegate;
        }

        @Override
        public void publish(MessageEnvelope envelope) {
            if (failPublish) {
                throw new IllegalStateException("Simulated broker failure");
            }
            delegate.publish(envelope);
        }

        @Override
        public void deadLetter(MessageEnvelope envelope, String errorCode) {
            if (failDeadLetter) {
                throw new IllegalStateException("Simulated dead-letter failure");
            }
            delegate.deadLetter(envelope, errorCode);
        }
    }

    static class AtomicProbe {
        private final JdbcTemplate jdbcTemplate;
        private final OutboxWriter writer;
        private final Clock clock;

        AtomicProbe(JdbcTemplate jdbcTemplate, OutboxWriter writer, Clock clock) {
            this.jdbcTemplate = jdbcTemplate;
            this.writer = writer;
            this.clock = clock;
        }

        @Transactional
        public void insertJournalThenFailOutbox(UUID userId) {
            UUID journalId = UUID.randomUUID();
            Instant now = clock.instant();
            jdbcTemplate.update(
                    """
                    INSERT INTO journal_entries (
                        id, user_id, content_text, content_format, mood_score, status,
                        journal_version, occurred_at, entry_date, timezone_at_entry,
                        is_favorite, created_at, updated_at, version)
                    VALUES (?, ?, 'atomic probe', 'PLAIN_TEXT', 5, 'SAVED', 1, ?, ?, 'UTC', FALSE, ?, ?, 0)
                    """,
                    journalId,
                    userId,
                    Timestamp.from(now),
                    LocalDate.ofInstant(now, java.time.ZoneOffset.UTC),
                    Timestamp.from(now),
                    Timestamp.from(now));
            writer.append("X".repeat(51), journalId, "journal.created", 1, Map.of());
        }
    }
}

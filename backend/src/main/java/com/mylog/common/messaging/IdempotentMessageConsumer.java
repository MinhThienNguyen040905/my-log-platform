package com.mylog.common.messaging;

import com.mylog.common.outbox.MessageEnvelope;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class IdempotentMessageConsumer {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public IdempotentMessageConsumer(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public void consume(
            String consumerName,
            MessageEnvelope envelope,
            long deliveryTag,
            Channel channel,
            MessageAction action)
            throws IOException {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                int inserted = jdbcTemplate.update(
                        """
                        INSERT INTO processed_messages (consumer_name, message_id, processed_at)
                        VALUES (?, ?, ?)
                        ON CONFLICT (consumer_name, message_id) DO NOTHING
                        """,
                        consumerName,
                        envelope.messageId(),
                        Timestamp.from(clock.instant()));
                if (inserted == 1) {
                    action.process(envelope);
                }
            });
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException exception) {
            channel.basicNack(deliveryTag, false, false);
            throw exception;
        }
    }

    @FunctionalInterface
    public interface MessageAction {
        void process(MessageEnvelope envelope);
    }
}

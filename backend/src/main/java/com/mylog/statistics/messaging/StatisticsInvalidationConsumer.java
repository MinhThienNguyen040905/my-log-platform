package com.mylog.statistics.messaging;

import com.mylog.common.messaging.IdempotentMessageConsumer;
import com.mylog.common.messaging.MessagingTopology;
import com.mylog.common.outbox.MessageEnvelope;
import com.mylog.common.outbox.OutboxWriter;
import com.mylog.statistics.service.StatisticsService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class StatisticsInvalidationConsumer {

    private static final String CONSUMER = "statistics-invalidation-v1";

    private final ObjectMapper objectMapper;
    private final IdempotentMessageConsumer consumer;
    private final StatisticsService statistics;
    private final OutboxWriter outbox;

    public StatisticsInvalidationConsumer(
            ObjectMapper objectMapper, IdempotentMessageConsumer consumer,
            StatisticsService statistics, OutboxWriter outbox) {
        this.objectMapper = objectMapper;
        this.consumer = consumer;
        this.statistics = statistics;
        this.outbox = outbox;
    }

    @RabbitListener(
            queues = MessagingTopology.STATISTICS_QUEUE,
            autoStartup = "${mylog.statistics.consumer-enabled:true}")
    public void receive(Message message, Channel channel) throws IOException {
        MessageEnvelope envelope = objectMapper.readValue(message.getBody(), MessageEnvelope.class);
        consumer.consume(CONSUMER, envelope, message.getMessageProperties().getDeliveryTag(), channel, this::invalidate);
    }

    private void invalidate(MessageEnvelope envelope) {
        UUID userId = UUID.fromString(String.valueOf(envelope.payload().get("userId")));
        statistics.invalidate(userId);
        outbox.append("USER", userId, MessagingTopology.STATISTICS_UPDATED, 1,
                Map.of("userId", userId.toString()));
    }
}

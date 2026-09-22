package com.mylog.analysis.messaging;

import com.mylog.analysis.config.AiProperties;
import com.mylog.analysis.service.AnalysisCommandService;
import com.mylog.common.messaging.IdempotentMessageConsumer;
import com.mylog.common.messaging.MessagingTopology;
import com.mylog.common.outbox.MessageEnvelope;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AnalysisRequestedConsumer {

    private static final String CONSUMER = "analysis-job-enqueuer-v1";

    private final ObjectMapper objectMapper;
    private final IdempotentMessageConsumer consumer;
    private final AnalysisCommandService commands;

    public AnalysisRequestedConsumer(
            ObjectMapper objectMapper, IdempotentMessageConsumer consumer, AnalysisCommandService commands) {
        this.objectMapper = objectMapper;
        this.consumer = consumer;
        this.commands = commands;
    }

    @RabbitListener(
            queues = MessagingTopology.ANALYSIS_QUEUE,
            autoStartup = "${mylog.ai.consumer-enabled:true}")
    public void receive(Message message, Channel channel) throws IOException {
        MessageEnvelope envelope = objectMapper.readValue(message.getBody(), MessageEnvelope.class);
        consumer.consume(CONSUMER, envelope, message.getMessageProperties().getDeliveryTag(), channel, this::enqueue);
    }

    private void enqueue(MessageEnvelope envelope) {
        UUID journalId = UUID.fromString(String.valueOf(envelope.payload().get("journalId")));
        UUID userId = UUID.fromString(String.valueOf(envelope.payload().get("userId")));
        long version = Long.parseLong(String.valueOf(envelope.payload().get("journalVersion")));
        commands.enqueueRequestedAnalysis(userId, journalId, version);
    }
}

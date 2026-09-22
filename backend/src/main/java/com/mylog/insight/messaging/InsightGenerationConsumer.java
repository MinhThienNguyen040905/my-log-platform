package com.mylog.insight.messaging;

import com.mylog.common.messaging.IdempotentMessageConsumer;
import com.mylog.common.messaging.MessagingTopology;
import com.mylog.common.outbox.MessageEnvelope;
import com.mylog.insight.service.InsightService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class InsightGenerationConsumer {

    private static final String CONSUMER = "insight-generation-v1";

    private final ObjectMapper objectMapper;
    private final IdempotentMessageConsumer consumer;
    private final InsightService insights;

    public InsightGenerationConsumer(
            ObjectMapper objectMapper, IdempotentMessageConsumer consumer, InsightService insights) {
        this.objectMapper = objectMapper;
        this.consumer = consumer;
        this.insights = insights;
    }

    @RabbitListener(
            queues = MessagingTopology.INSIGHT_QUEUE,
            autoStartup = "${mylog.insight.consumer-enabled:true}")
    public void receive(Message message, Channel channel) throws IOException {
        MessageEnvelope envelope = objectMapper.readValue(message.getBody(), MessageEnvelope.class);
        consumer.consume(CONSUMER, envelope, message.getMessageProperties().getDeliveryTag(), channel, event ->
                insights.refresh(UUID.fromString(String.valueOf(event.payload().get("userId")))));
    }
}

package com.mylog.common.outbox;

import com.mylog.common.messaging.MessagingTopology;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RabbitOutboxTransport implements OutboxTransport {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;

    public RabbitOutboxTransport(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            OutboxProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.rabbitTemplate.setMandatory(true);
    }

    @Override
    public void publish(MessageEnvelope envelope) {
        send(MessagingTopology.EVENTS_EXCHANGE, envelope.eventType(), envelope, null);
    }

    @Override
    public void deadLetter(MessageEnvelope envelope, String errorCode) {
        send(MessagingTopology.DEAD_LETTER_EXCHANGE, "outbox.failed", envelope, errorCode);
    }

    private void send(
            String exchange,
            String routingKey,
            MessageEnvelope envelope,
            String errorCode) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        messageProperties.setMessageId(envelope.messageId().toString());
        messageProperties.setTimestamp(java.util.Date.from(envelope.occurredAt()));
        messageProperties.setHeader("eventType", envelope.eventType());
        messageProperties.setHeader("eventVersion", envelope.eventVersion());
        if (errorCode != null) {
            messageProperties.setHeader("failureCode", errorCode);
        }
        Message message = new Message(objectMapper.writeValueAsBytes(envelope), messageProperties);
        CorrelationData correlation = new CorrelationData(envelope.messageId().toString());
        rabbitTemplate.send(exchange, routingKey, message, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(
                    properties.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.ack() || correlation.getReturned() != null) {
                String reason = correlation.getReturned() == null
                        ? confirm.reason()
                        : "Message was returned as unroutable";
                throw new AmqpException(reason == null ? "Publisher confirm was negative" : reason);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AmqpException("Interrupted while waiting for publisher confirm", exception);
        } catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException exception) {
            throw new AmqpException("Publisher confirm was not received", exception);
        }
    }
}

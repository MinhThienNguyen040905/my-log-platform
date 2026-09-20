package com.mylog.shared.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    TopicExchange myLogEventsExchange() {
        return new TopicExchange(MessagingTopology.EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue analysisQueue() {
        return quorumQueue(MessagingTopology.ANALYSIS_QUEUE);
    }

    @Bean
    Queue reflectionQueue() {
        return quorumQueue(MessagingTopology.REFLECTION_QUEUE);
    }

    @Bean
    Queue statisticsQueue() {
        return quorumQueue(MessagingTopology.STATISTICS_QUEUE);
    }

    @Bean
    Queue insightQueue() {
        return quorumQueue(MessagingTopology.INSIGHT_QUEUE);
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(MessagingTopology.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding analysisBinding(Queue analysisQueue, TopicExchange myLogEventsExchange) {
        return BindingBuilder.bind(analysisQueue)
                .to(myLogEventsExchange)
                .with(MessagingTopology.JOURNAL_ANALYSIS_REQUESTED);
    }

    @Bean
    Binding reflectionBinding(Queue reflectionQueue, TopicExchange myLogEventsExchange) {
        return BindingBuilder.bind(reflectionQueue)
                .to(myLogEventsExchange)
                .with(MessagingTopology.JOURNAL_ANALYSIS_COMPLETED);
    }

    @Bean
    Binding statisticsAnalysisBinding(Queue statisticsQueue, TopicExchange myLogEventsExchange) {
        return BindingBuilder.bind(statisticsQueue)
                .to(myLogEventsExchange)
                .with(MessagingTopology.JOURNAL_ANALYSIS_COMPLETED);
    }

    @Bean
    Binding statisticsCorrectionBinding(Queue statisticsQueue, TopicExchange myLogEventsExchange) {
        return BindingBuilder.bind(statisticsQueue)
                .to(myLogEventsExchange)
                .with(MessagingTopology.JOURNAL_CORRECTED);
    }

    @Bean
    Binding insightBinding(Queue insightQueue, TopicExchange myLogEventsExchange) {
        return BindingBuilder.bind(insightQueue)
                .to(myLogEventsExchange)
                .with(MessagingTopology.STATISTICS_UPDATED);
    }

    private Queue quorumQueue(String name) {
        return QueueBuilder.durable(name)
                .quorum()
                .deadLetterExchange(MessagingTopology.EVENTS_EXCHANGE)
                .deadLetterRoutingKey("dead." + name)
                .build();
    }
}

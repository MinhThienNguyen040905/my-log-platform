package com.mylog.platform.outbox;

import com.mylog.platform.id.IdGenerator;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "mylog.identity", name = "enabled", havingValue = "true")
class JpaOutboxPublisher implements OutboxPublisher {
    private final EntityManager entityManager;
    private final IdGenerator ids;

    JpaOutboxPublisher(EntityManager entityManager, IdGenerator ids) {
        this.entityManager = entityManager;
        this.ids = ids;
    }

    @Override public void publish(String aggregateType, UUID aggregateId, String eventType, int contentVersion, Instant now) {
        OutboxEvent event = new OutboxEvent();
        event.id = ids.next();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.eventVersion = 1;
        event.payload = Map.of("aggregateId", aggregateId.toString(), "contentVersion", contentVersion);
        event.status = "PENDING";
        event.availableAt = now;
        event.createdAt = now;
        entityManager.persist(event);
    }
}

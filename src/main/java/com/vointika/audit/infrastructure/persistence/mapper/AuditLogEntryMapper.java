package com.vointika.audit.infrastructure.persistence.mapper;

import com.vointika.audit.domain.entity.AuditLogEntry;
import com.vointika.audit.domain.projection.AuditLogListItem;
import com.vointika.audit.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AuditLogEntryMapper {

    // Jackson 3 (tools.jackson) — the auto-configured Spring bean; there is
    // no Jackson-2 (com.fasterxml) ObjectMapper bean in a Boot 4 context.
    private final ObjectMapper objectMapper;

    public AuditLogEntryMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuditLogJpaEntity toJpa(AuditLogEntry entry) {
        return new AuditLogJpaEntity(
                entry.getId(),
                entry.getTourOperatorId(),
                entry.getActor().type(),
                entry.getActor().id(),
                entry.getActorName(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getAction(),
                serialize(entry.getDetails()),
                serialize(entry.getChanges()),
                entry.getRequestId(),
                entry.getCreatedAt()
        );
    }

    public static AuditLogListItem toListItem(AuditLogJpaEntity entity) {
        return new AuditLogListItem(
                entity.getId(),
                entity.getActorType(),
                entity.getActorId(),
                entity.getActorName(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getAction(),
                entity.getDetails(),
                entity.getChanges(),
                entity.getRequestId(),
                entity.getCreatedAt()
        );
    }

    private String serialize(Object payload) {
        return payload == null ? null : objectMapper.writeValueAsString(payload);
    }
}

package com.vointika.audit.infrastructure.persistence.repository;

import com.vointika.audit.application.usecase.ListAuditLogUseCase;
import com.vointika.audit.domain.entity.AuditLogEntry;
import com.vointika.audit.domain.projection.AuditLogListItem;
import com.vointika.audit.domain.repository.AuditLogEntryRepository;
import com.vointika.audit.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.vointika.audit.infrastructure.persistence.mapper.AuditLogEntryMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AuditLogEntryRepositoryImpl implements AuditLogEntryRepository {

    private final AuditLogJpaRepository jpa;
    private final AuditLogEntryMapper mapper;
    private final CriteriaListExecutor listExecutor;

    public AuditLogEntryRepositoryImpl(AuditLogJpaRepository jpa,
                                       AuditLogEntryMapper mapper,
                                       CriteriaListExecutor listExecutor) {
        this.jpa = jpa;
        this.mapper = mapper;
        this.listExecutor = listExecutor;
    }

    @Override
    public void append(AuditLogEntry entry) {
        jpa.save(mapper.toJpa(entry));
    }

    @Override
    public CursorPage<AuditLogListItem> list(ListQuery query) {
        return listExecutor.list(
                AuditLogJpaEntity.class,
                ListAuditLogUseCase.SCHEMA,
                query,
                AuditLogEntryMapper::toListItem
        );
    }

    @Override
    public Optional<AuditLogListItem> findByIdAndTourOperatorId(UUID entryId, UUID tourOperatorId) {
        return jpa.findByIdAndTourOperatorId(entryId, tourOperatorId)
                .map(AuditLogEntryMapper::toListItem);
    }
}

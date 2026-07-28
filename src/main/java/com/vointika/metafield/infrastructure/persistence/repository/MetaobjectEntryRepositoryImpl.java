package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.application.usecase.ListMetaobjectEntriesUseCase;
import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.entity.MetaobjectEntryValue;
import com.vointika.metafield.domain.projection.MetaobjectEntryListItem;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectEntryJpaEntity;
import com.vointika.metafield.infrastructure.persistence.mapper.MetaobjectMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MetaobjectEntryRepositoryImpl implements MetaobjectEntryRepository {

    private final MetaobjectEntryJpaRepository entryJpa;
    private final MetaobjectEntryValueJpaRepository valueJpa;
    private final CriteriaListExecutor listExecutor;

    public MetaobjectEntryRepositoryImpl(MetaobjectEntryJpaRepository entryJpa,
                                         MetaobjectEntryValueJpaRepository valueJpa,
                                         CriteriaListExecutor listExecutor) {
        this.entryJpa = entryJpa;
        this.valueJpa = valueJpa;
        this.listExecutor = listExecutor;
    }

    @Override
    public MetaobjectEntry save(MetaobjectEntry entry) {
        return MetaobjectMapper.toDomain(entryJpa.save(MetaobjectMapper.toJpa(entry)));
    }

    @Override
    public Optional<MetaobjectEntry> findByIdAndTourOperatorId(UUID entryId, UUID tourOperatorId) {
        return entryJpa.findByIdAndTourOperatorId(entryId, tourOperatorId)
                .map(MetaobjectMapper::toDomain);
    }

    @Override
    public boolean existsByDefinitionIdAndHandle(UUID definitionId, String handle) {
        return entryJpa.existsByDefinitionIdAndHandle(definitionId, handle);
    }

    @Override
    public CursorPage<MetaobjectEntryListItem> list(ListQuery query) {
        return listExecutor.list(
                MetaobjectEntryJpaEntity.class,
                ListMetaobjectEntriesUseCase.SCHEMA,
                query,
                MetaobjectMapper::toEntryListItem);
    }

    @Override
    public void delete(UUID entryId) {
        entryJpa.deleteById(entryId);
    }

    @Override
    public MetaobjectEntryValue saveValue(MetaobjectEntryValue value) {
        return MetaobjectMapper.toDomain(valueJpa.save(MetaobjectMapper.toJpa(value)));
    }

    @Override
    public List<MetaobjectEntryValue> valuesOf(UUID entryId) {
        return valueJpa.findByEntryId(entryId).stream()
                .map(MetaobjectMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<MetaobjectEntryValue> findValue(UUID entryId, UUID fieldDefinitionId) {
        return valueJpa.findByEntryIdAndFieldDefinitionId(entryId, fieldDefinitionId)
                .map(MetaobjectMapper::toDomain);
    }

    @Override
    public void deleteValue(UUID valueId) {
        valueJpa.deleteById(valueId);
    }
}

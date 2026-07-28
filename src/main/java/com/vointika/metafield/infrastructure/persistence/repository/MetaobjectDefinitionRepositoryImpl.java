package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.application.usecase.ListMetaobjectDefinitionsUseCase;
import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.projection.MetaobjectDefinitionListItem;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectDefinitionJpaEntity;
import com.vointika.metafield.infrastructure.persistence.mapper.MetaobjectMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MetaobjectDefinitionRepositoryImpl implements MetaobjectDefinitionRepository {

    private final MetaobjectDefinitionJpaRepository definitionJpa;
    private final MetaobjectFieldJpaRepository fieldJpa;
    private final CriteriaListExecutor listExecutor;

    public MetaobjectDefinitionRepositoryImpl(MetaobjectDefinitionJpaRepository definitionJpa,
                                              MetaobjectFieldJpaRepository fieldJpa,
                                              CriteriaListExecutor listExecutor) {
        this.definitionJpa = definitionJpa;
        this.fieldJpa = fieldJpa;
        this.listExecutor = listExecutor;
    }

    @Override
    public MetaobjectDefinition save(MetaobjectDefinition definition) {
        return MetaobjectMapper.toDomain(definitionJpa.save(MetaobjectMapper.toJpa(definition)));
    }

    @Override
    public Optional<MetaobjectDefinition> findByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId) {
        return definitionJpa.findByIdAndTourOperatorId(definitionId, tourOperatorId)
                .map(MetaobjectMapper::toDomain);
    }

    @Override
    public boolean existsByTourOperatorIdAndType(UUID tourOperatorId, String type) {
        return definitionJpa.existsByTourOperatorIdAndType(tourOperatorId, type);
    }

    @Override
    public CursorPage<MetaobjectDefinitionListItem> list(ListQuery query) {
        return listExecutor.list(
                MetaobjectDefinitionJpaEntity.class,
                ListMetaobjectDefinitionsUseCase.SCHEMA,
                query,
                MetaobjectMapper::toListItem);
    }

    @Override
    public void delete(UUID definitionId) {
        definitionJpa.deleteById(definitionId);
    }

    @Override
    public MetaobjectField saveField(MetaobjectField field) {
        return MetaobjectMapper.toDomain(fieldJpa.save(MetaobjectMapper.toJpa(field)));
    }

    @Override
    public List<MetaobjectField> fieldsOf(UUID definitionId) {
        return fieldJpa.findByDefinitionIdOrderByPosition(definitionId).stream()
                .map(MetaobjectMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<MetaobjectField> findField(UUID definitionId, String key) {
        return fieldJpa.findByDefinitionIdAndKey(definitionId, key)
                .map(MetaobjectMapper::toDomain);
    }

    @Override
    public boolean existsField(UUID definitionId, String key) {
        return fieldJpa.existsByDefinitionIdAndKey(definitionId, key);
    }

    @Override
    public long countFields(UUID definitionId) {
        return fieldJpa.countByDefinitionId(definitionId);
    }

    @Override
    public void deleteField(UUID fieldId) {
        fieldJpa.deleteById(fieldId);
    }
}

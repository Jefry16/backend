package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.application.usecase.ListMetafieldDefinitionsUseCase;
import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.projection.MetafieldDefinitionListItem;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.infrastructure.persistence.entity.MetafieldDefinitionJpaEntity;
import com.vointika.metafield.infrastructure.persistence.mapper.MetafieldDefinitionMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MetafieldDefinitionRepositoryImpl implements MetafieldDefinitionRepository {

    private final MetafieldDefinitionJpaRepository jpa;
    private final CriteriaListExecutor listExecutor;

    public MetafieldDefinitionRepositoryImpl(MetafieldDefinitionJpaRepository jpa,
                                             CriteriaListExecutor listExecutor) {
        this.jpa = jpa;
        this.listExecutor = listExecutor;
    }

    @Override
    public MetafieldDefinition save(MetafieldDefinition definition) {
        return MetafieldDefinitionMapper.toDomain(
                jpa.save(MetafieldDefinitionMapper.toJpa(definition)));
    }

    @Override
    public Optional<MetafieldDefinition> findByIdAndTourOperatorId(UUID definitionId, UUID tourOperatorId) {
        return jpa.findByIdAndTourOperatorId(definitionId, tourOperatorId)
                .map(MetafieldDefinitionMapper::toDomain);
    }

    @Override
    public Optional<MetafieldDefinition> findByIdentity(
            UUID tourOperatorId, MetafieldOwnerType ownerType, String namespace, String key) {
        return jpa.findByTourOperatorIdAndOwnerTypeAndNamespaceAndKey(
                        tourOperatorId, ownerType, namespace, key)
                .map(MetafieldDefinitionMapper::toDomain);
    }

    @Override
    public boolean existsByIdentity(
            UUID tourOperatorId, MetafieldOwnerType ownerType, String namespace, String key) {
        return jpa.existsByTourOperatorIdAndOwnerTypeAndNamespaceAndKey(
                tourOperatorId, ownerType, namespace, key);
    }

    @Override
    public CursorPage<MetafieldDefinitionListItem> list(ListQuery query) {
        return listExecutor.list(
                MetafieldDefinitionJpaEntity.class,
                ListMetafieldDefinitionsUseCase.SCHEMA,
                query,
                MetafieldDefinitionMapper::toListItem);
    }

    @Override
    public void delete(UUID definitionId) {
        jpa.deleteById(definitionId);
    }
}

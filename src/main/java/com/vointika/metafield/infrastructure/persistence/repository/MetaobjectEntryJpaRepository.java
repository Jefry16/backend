package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.domain.projection.PublishedMetaobjectField;
import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetaobjectEntryJpaRepository
        extends JpaRepository<MetaobjectEntryJpaEntity, UUID> {

    Optional<MetaobjectEntryJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByDefinitionIdAndHandle(UUID definitionId, String handle);

    boolean existsByIdAndDefinitionIdAndTourOperatorId(UUID id, UUID definitionId, UUID tourOperatorId);

    /**
     * The storefront's batch resolve for {@code metaobject_reference} metafields.
     *
     * <p><b>Three filters, and each is load-bearing.</b> {@code tourOperatorId}
     * keeps a forged id from reaching another operator's content;
     * {@code published} is the visibility gate, so a draft entry resolves to
     * nothing and the metafield is dropped; the id set is the batch itself.
     *
     * <p>Joining <b>from</b> the value rows makes the unset-field rule fall out
     * of the join: a field with no row simply produces none, which is the same
     * "unset fields have no row" the table was designed around. An outer join
     * would invent an empty field per definition instead.
     *
     * <p>Ordered by entry then field {@code position}, so a theme reading the
     * fields gets the operator's own arrangement rather than insertion order.
     */
    @Query("""
            SELECT new com.vointika.metafield.domain.projection.PublishedMetaobjectField(
                e.id, d.type, e.handle, e.name, f.key, f.type, v.value)
            FROM MetaobjectEntryValueJpaEntity v
            JOIN MetaobjectEntryJpaEntity e ON e.id = v.entryId
            JOIN MetaobjectDefinitionJpaEntity d ON d.id = e.definitionId
            JOIN MetaobjectFieldJpaEntity f ON f.id = v.fieldDefinitionId
            WHERE e.tourOperatorId = :tourOperatorId
              AND e.id IN :entryIds
              AND e.published = TRUE
            ORDER BY e.id, f.position
            """)
    List<PublishedMetaobjectField> findPublishedFields(@Param("tourOperatorId") UUID tourOperatorId,
                                                       @Param("entryIds") Collection<UUID> entryIds);
}

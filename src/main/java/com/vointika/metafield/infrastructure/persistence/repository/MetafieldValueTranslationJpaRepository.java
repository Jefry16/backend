package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository.TranslatedValue;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.infrastructure.persistence.entity.MetafieldValueTranslationId;
import com.vointika.metafield.infrastructure.persistence.entity.MetafieldValueTranslationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Every query here reaches the translation rows <b>through the definition</b>,
 * which is the only table carrying the tenant. The translation row has no
 * operator column and must not grow one: it would be a third copy of a fact the
 * definition already owns, and the join is what keeps one operator's edit from
 * touching another's row.
 */
public interface MetafieldValueTranslationJpaRepository
        extends JpaRepository<MetafieldValueTranslationJpaEntity, MetafieldValueTranslationId> {

    @Query("""
            SELECT new com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository$TranslatedValue(
                d.namespace, d.key, t.value)
            FROM MetafieldValueTranslationJpaEntity t
            JOIN MetafieldValueJpaEntity v ON v.id = t.metafieldValueId
            JOIN MetafieldDefinitionJpaEntity d ON d.id = v.definitionId
            WHERE d.tourOperatorId = :tourOperatorId
              AND d.ownerType = :ownerType
              AND v.ownerId = :ownerId
              AND t.locale = :locale
            ORDER BY d.namespace, d.key
            """)
    List<TranslatedValue> findForOwner(@Param("tourOperatorId") UUID tourOperatorId,
                                       @Param("ownerType") MetafieldOwnerType ownerType,
                                       @Param("ownerId") UUID ownerId,
                                       @Param("locale") String locale);

    @Query("""
            SELECT DISTINCT t.locale
            FROM MetafieldValueTranslationJpaEntity t
            JOIN MetafieldValueJpaEntity v ON v.id = t.metafieldValueId
            JOIN MetafieldDefinitionJpaEntity d ON d.id = v.definitionId
            WHERE d.tourOperatorId = :tourOperatorId
              AND d.ownerType = :ownerType
              AND v.ownerId = :ownerId
            ORDER BY t.locale
            """)
    List<String> findLocalesForOwner(@Param("tourOperatorId") UUID tourOperatorId,
                                     @Param("ownerType") MetafieldOwnerType ownerType,
                                     @Param("ownerId") UUID ownerId);

    /**
     * Clearing a whole locale. Written as a bulk delete over a subquery rather
     * than a read-then-delete: the set is defined by the same join every other
     * query here uses, and loading rows only to delete them buys nothing.
     */
    @Modifying
    @Query("""
            DELETE FROM MetafieldValueTranslationJpaEntity t
            WHERE t.locale = :locale
              AND t.metafieldValueId IN (
                  SELECT v.id FROM MetafieldValueJpaEntity v
                  JOIN MetafieldDefinitionJpaEntity d ON d.id = v.definitionId
                  WHERE d.tourOperatorId = :tourOperatorId
                    AND d.ownerType = :ownerType
                    AND v.ownerId = :ownerId)
            """)
    int deleteForOwner(@Param("tourOperatorId") UUID tourOperatorId,
                       @Param("ownerType") MetafieldOwnerType ownerType,
                       @Param("ownerId") UUID ownerId,
                       @Param("locale") String locale);
}

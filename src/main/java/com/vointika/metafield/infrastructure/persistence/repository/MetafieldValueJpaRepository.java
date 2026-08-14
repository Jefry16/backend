package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.domain.projection.MetafieldValueWithDefinition;
import com.vointika.metafield.domain.projection.TranslatableMetafieldValue;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.infrastructure.persistence.entity.MetafieldValueJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetafieldValueJpaRepository extends JpaRepository<MetafieldValueJpaEntity, UUID> {

    Optional<MetafieldValueJpaEntity> findByDefinitionIdAndOwnerId(UUID definitionId, UUID ownerId);

    /**
     * The per-resource read: this owner's values joined with their definitions,
     * scoped through the operator-owned definition (the value row itself has no
     * tenant column). Stable namespace.key ordering for the editor.
     */
    @Query("""
            SELECT new com.vointika.metafield.domain.projection.MetafieldValueWithDefinition(
                d.namespace, d.key, d.type, d.name, v.value, v.updatedAt)
            FROM MetafieldValueJpaEntity v
            JOIN MetafieldDefinitionJpaEntity d ON d.id = v.definitionId
            WHERE d.tourOperatorId = :tourOperatorId
              AND d.ownerType = :ownerType
              AND v.ownerId = :ownerId
            ORDER BY d.namespace, d.key
            """)
    List<MetafieldValueWithDefinition> listForOwner(
            @Param("tourOperatorId") UUID tourOperatorId,
            @Param("ownerType") MetafieldOwnerType ownerType,
            @Param("ownerId") UUID ownerId);

    /**
     * The same read with one locale's translations overlaid — the storefront's,
     * and only the storefront's.
     *
     * <p><b>It is a second query rather than a nullable locale on the one above
     * on purpose.</b> The admin editor shows what the operator typed; overlaying
     * there would make a translated field look canonical and the next save would
     * write the translation back over the original. Two readers, two questions.
     *
     * <p>The overlay is {@code COALESCE} over a LEFT JOIN because the fallback is
     * <b>a missing row</b>, not a null column — a metafield value is one row with
     * one value, so there is no per-field nullability to fall through (the
     * column-shaped overlay every other translation table uses).
     *
     * <p>Ordering repeats {@code listForOwner}'s deliberately: it is the query's
     * promise to its caller, and a storefront payload that reshuffles between
     * requests is a bug report.
     */
    @Query("""
            SELECT new com.vointika.metafield.domain.projection.MetafieldValueWithDefinition(
                d.namespace, d.key, d.type, d.name, COALESCE(t.value, v.value), v.updatedAt)
            FROM MetafieldValueJpaEntity v
            JOIN MetafieldDefinitionJpaEntity d ON d.id = v.definitionId
            LEFT JOIN MetafieldValueTranslationJpaEntity t
                   ON t.metafieldValueId = v.id AND t.locale = :locale
            WHERE d.tourOperatorId = :tourOperatorId
              AND d.ownerType = :ownerType
              AND v.ownerId = :ownerId
            ORDER BY d.namespace, d.key
            """)
    List<MetafieldValueWithDefinition> listForOwnerLocalized(
            @Param("tourOperatorId") UUID tourOperatorId,
            @Param("ownerType") MetafieldOwnerType ownerType,
            @Param("ownerId") UUID ownerId,
            @Param("locale") String locale);

    /**
     * Everything a translation payload needs to resolve itself, in one query:
     * the qualified key, the type that decides translatability, and the value id
     * a translation row hangs off. Values only — a definition with nothing stored
     * has nothing to translate, and the overlay joins FROM the stored value.
     */
    @Query("""
            SELECT new com.vointika.metafield.domain.projection.TranslatableMetafieldValue(
                d.namespace, d.key, d.type, v.id)
            FROM MetafieldValueJpaEntity v
            JOIN MetafieldDefinitionJpaEntity d ON d.id = v.definitionId
            WHERE d.tourOperatorId = :tourOperatorId
              AND d.ownerType = :ownerType
              AND v.ownerId = :ownerId
            """)
    List<TranslatableMetafieldValue> listTranslatableForOwner(
            @Param("tourOperatorId") UUID tourOperatorId,
            @Param("ownerType") MetafieldOwnerType ownerType,
            @Param("ownerId") UUID ownerId);

    /**
     * Deletes every metaobject_reference value pointing at one entry (runs in
     * the entry-delete tx so no dangling references survive). flushAutomatically
     * per the house @Modifying convention — a pending same-tx save on another
     * table must not be discarded by the clear.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM MetafieldValueJpaEntity v
            WHERE v.value = :entryId
              AND EXISTS (
                  SELECT 1 FROM MetafieldDefinitionJpaEntity d
                  WHERE d.id = v.definitionId
                    AND d.type = com.vointika.metafield.domain.valueobject.MetafieldType.METAOBJECT_REFERENCE)
            """)
    void deleteReferencesTo(@Param("entryId") String entryId);

    /**
     * Deletes every value a deleted owner held (runs in the owner's delete tx).
     * A bulk delete rather than Spring Data's derived {@code deleteByOwnerId},
     * which loads each row to remove it one at a time; {@code
     * idx_metafield_values_owner_id} makes this one indexed statement. Same
     * flush/clear pair as above and for the same reason.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MetafieldValueJpaEntity v WHERE v.ownerId = :ownerId")
    void deleteByOwnerId(@Param("ownerId") UUID ownerId);
}

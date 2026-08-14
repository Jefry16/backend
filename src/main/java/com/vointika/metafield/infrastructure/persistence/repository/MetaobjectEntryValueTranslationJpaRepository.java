package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.domain.repository.MetaobjectEntryValueTranslationRepository.TranslatedField;
import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectEntryValueTranslationId;
import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectEntryValueTranslationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Every query reaches the translation rows through the entry's value rows, which
 * is what scopes them — the translation row carries no entry id of its own, and
 * must not grow one.
 */
public interface MetaobjectEntryValueTranslationJpaRepository
        extends JpaRepository<MetaobjectEntryValueTranslationJpaEntity, MetaobjectEntryValueTranslationId> {

    @Query("""
            SELECT new com.vointika.metafield.domain.repository.MetaobjectEntryValueTranslationRepository$TranslatedField(
                f.key, t.value)
            FROM MetaobjectEntryValueTranslationJpaEntity t
            JOIN MetaobjectEntryValueJpaEntity v ON v.id = t.entryValueId
            JOIN MetaobjectFieldJpaEntity f ON f.id = v.fieldDefinitionId
            WHERE v.entryId = :entryId
              AND t.locale = :locale
            ORDER BY f.position
            """)
    List<TranslatedField> findForEntry(@Param("entryId") UUID entryId,
                                       @Param("locale") String locale);

    @Query("""
            SELECT DISTINCT t.locale
            FROM MetaobjectEntryValueTranslationJpaEntity t
            JOIN MetaobjectEntryValueJpaEntity v ON v.id = t.entryValueId
            WHERE v.entryId = :entryId
            ORDER BY t.locale
            """)
    List<String> findLocalesForEntry(@Param("entryId") UUID entryId);

    @Modifying
    @Query("""
            DELETE FROM MetaobjectEntryValueTranslationJpaEntity t
            WHERE t.locale = :locale
              AND t.entryValueId IN (
                  SELECT v.id FROM MetaobjectEntryValueJpaEntity v WHERE v.entryId = :entryId)
            """)
    int deleteForEntry(@Param("entryId") UUID entryId, @Param("locale") String locale);
}

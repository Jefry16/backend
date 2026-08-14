package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.domain.entity.MetaobjectEntryValueTranslation;
import com.vointika.metafield.domain.repository.MetaobjectEntryValueTranslationRepository;
import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectEntryValueTranslationId;
import com.vointika.metafield.infrastructure.persistence.entity.MetaobjectEntryValueTranslationJpaEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class MetaobjectEntryValueTranslationRepositoryImpl
        implements MetaobjectEntryValueTranslationRepository {

    private final MetaobjectEntryValueTranslationJpaRepository jpa;

    public MetaobjectEntryValueTranslationRepositoryImpl(
            MetaobjectEntryValueTranslationJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** A replace keeps who wrote it first and moves only {@code updatedAt}. */
    @Override
    public void upsert(MetaobjectEntryValueTranslation translation) {
        MetaobjectEntryValueTranslationJpaEntity existing = jpa
                .findById(new MetaobjectEntryValueTranslationId(
                        translation.entryValueId(), translation.locale()))
                .orElse(null);

        jpa.save(existing == null
                ? new MetaobjectEntryValueTranslationJpaEntity(
                        translation.entryValueId(), translation.locale(), translation.value(),
                        translation.createdBy(), translation.createdAt(), translation.updatedAt())
                : new MetaobjectEntryValueTranslationJpaEntity(
                        existing.getEntryValueId(), existing.getLocale(), translation.value(),
                        existing.getCreatedBy(), existing.getCreatedAt(), Instant.now()));
    }

    @Override
    public boolean delete(UUID entryValueId, String locale) {
        MetaobjectEntryValueTranslationId id =
                new MetaobjectEntryValueTranslationId(entryValueId, locale);
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }

    @Override
    public int deleteForEntry(UUID entryId, String locale) {
        return jpa.deleteForEntry(entryId, locale);
    }

    @Override
    public List<TranslatedField> findForEntry(UUID entryId, String locale) {
        return jpa.findForEntry(entryId, locale);
    }

    @Override
    public List<String> findLocalesForEntry(UUID entryId) {
        return jpa.findLocalesForEntry(entryId);
    }
}

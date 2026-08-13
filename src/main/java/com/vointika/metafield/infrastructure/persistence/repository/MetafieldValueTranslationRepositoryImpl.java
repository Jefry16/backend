package com.vointika.metafield.infrastructure.persistence.repository;

import com.vointika.metafield.domain.entity.MetafieldValueTranslation;
import com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.infrastructure.persistence.entity.MetafieldValueTranslationId;
import com.vointika.metafield.infrastructure.persistence.entity.MetafieldValueTranslationJpaEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class MetafieldValueTranslationRepositoryImpl implements MetafieldValueTranslationRepository {

    private final MetafieldValueTranslationJpaRepository jpa;

    public MetafieldValueTranslationRepositoryImpl(MetafieldValueTranslationJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * {@code createdBy}/{@code createdAt} belong to whoever wrote the row first,
     * so a replace keeps them and moves only {@code updatedAt} — the same shape
     * {@code MetafieldValue.changeValue} has.
     */
    @Override
    public void upsert(MetafieldValueTranslation translation) {
        MetafieldValueTranslationJpaEntity existing = jpa
                .findById(new MetafieldValueTranslationId(
                        translation.metafieldValueId(), translation.locale()))
                .orElse(null);

        jpa.save(existing == null
                ? new MetafieldValueTranslationJpaEntity(
                        translation.metafieldValueId(), translation.locale(), translation.value(),
                        translation.createdBy(), translation.createdAt(), translation.updatedAt())
                : new MetafieldValueTranslationJpaEntity(
                        existing.getMetafieldValueId(), existing.getLocale(), translation.value(),
                        existing.getCreatedBy(), existing.getCreatedAt(), Instant.now()));
    }

    @Override
    public boolean delete(UUID metafieldValueId, String locale) {
        MetafieldValueTranslationId id = new MetafieldValueTranslationId(metafieldValueId, locale);
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }

    @Override
    public int deleteForOwner(UUID tourOperatorId, MetafieldOwnerType ownerType,
                              UUID ownerId, String locale) {
        return jpa.deleteForOwner(tourOperatorId, ownerType, ownerId, locale);
    }

    @Override
    public List<TranslatedValue> findForOwner(UUID tourOperatorId, MetafieldOwnerType ownerType,
                                              UUID ownerId, String locale) {
        return jpa.findForOwner(tourOperatorId, ownerType, ownerId, locale);
    }

    @Override
    public List<String> findLocalesForOwner(UUID tourOperatorId, MetafieldOwnerType ownerType,
                                            UUID ownerId) {
        return jpa.findLocalesForOwner(tourOperatorId, ownerType, ownerId);
    }
}

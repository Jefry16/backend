package com.vointika.audience.domain.repository;

import com.vointika.audience.domain.entity.AudienceTranslation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AudienceTranslationRepository {

    /** Create-or-replace the whole (audience, locale) overlay row. */
    AudienceTranslation upsert(AudienceTranslation translation);

    Optional<AudienceTranslation> findByAudienceIdAndLocale(UUID audienceId, String locale);

    /** All translated locales for an audience (one row each). */
    List<AudienceTranslation> findAllByAudienceId(UUID audienceId);

    /** Removes the (audience, locale) row if present; idempotent. */
    void deleteByAudienceIdAndLocale(UUID audienceId, String locale);
}

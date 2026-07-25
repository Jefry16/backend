package com.vointika.audience.infrastructure.persistence.repository;

import com.vointika.audience.infrastructure.persistence.entity.AudienceTranslationId;
import com.vointika.audience.infrastructure.persistence.entity.AudienceTranslationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AudienceTranslationJpaRepository
        extends JpaRepository<AudienceTranslationJpaEntity, AudienceTranslationId> {

    Optional<AudienceTranslationJpaEntity> findByAudienceIdAndLocale(UUID audienceId, String locale);

    List<AudienceTranslationJpaEntity> findByAudienceId(UUID audienceId);
}

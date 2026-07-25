package com.vointika.audience.infrastructure.persistence.repository;

import com.vointika.audience.domain.entity.AudienceTranslation;
import com.vointika.audience.domain.repository.AudienceTranslationRepository;
import com.vointika.audience.infrastructure.persistence.entity.AudienceTranslationId;
import com.vointika.audience.infrastructure.persistence.mapper.AudienceTranslationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AudienceTranslationRepositoryImpl implements AudienceTranslationRepository {

    private final AudienceTranslationJpaRepository jpaRepository;

    public AudienceTranslationRepositoryImpl(AudienceTranslationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AudienceTranslation upsert(AudienceTranslation translation) {
        // save() merges on the assigned composite id → create-or-replace the row.
        return AudienceTranslationMapper.toDomain(
                jpaRepository.save(AudienceTranslationMapper.toJpa(translation)));
    }

    @Override
    public Optional<AudienceTranslation> findByAudienceIdAndLocale(UUID audienceId, String locale) {
        return jpaRepository.findByAudienceIdAndLocale(audienceId, locale)
                .map(AudienceTranslationMapper::toDomain);
    }

    @Override
    public List<AudienceTranslation> findAllByAudienceId(UUID audienceId) {
        return jpaRepository.findByAudienceId(audienceId).stream()
                .map(AudienceTranslationMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByAudienceIdAndLocale(UUID audienceId, String locale) {
        AudienceTranslationId id = new AudienceTranslationId(audienceId, locale);
        if (jpaRepository.existsById(id)) {
            jpaRepository.deleteById(id);
        }
    }
}

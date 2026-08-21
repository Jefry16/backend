package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.infrastructure.persistence.entity.CategoryTranslationId;
import com.vointika.experience.infrastructure.persistence.entity.CategoryTranslationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryTranslationJpaRepository
        extends JpaRepository<CategoryTranslationJpaEntity, CategoryTranslationId> {

    Optional<CategoryTranslationJpaEntity> findByCategoryIdAndLocale(UUID categoryId, String locale);

    List<CategoryTranslationJpaEntity> findByCategoryId(UUID categoryId);
}

package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.domain.entity.CategoryTranslation;
import com.vointika.experience.domain.repository.CategoryTranslationRepository;
import com.vointika.experience.infrastructure.persistence.entity.CategoryTranslationId;
import com.vointika.experience.infrastructure.persistence.mapper.CategoryTranslationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CategoryTranslationRepositoryImpl implements CategoryTranslationRepository {

    private final CategoryTranslationJpaRepository jpaRepository;

    public CategoryTranslationRepositoryImpl(CategoryTranslationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CategoryTranslation upsert(CategoryTranslation translation) {
        // save() merges on the assigned composite id → create-or-replace the row.
        return CategoryTranslationMapper.toDomain(
                jpaRepository.save(CategoryTranslationMapper.toJpa(translation)));
    }

    @Override
    public Optional<CategoryTranslation> findByCategoryIdAndLocale(UUID categoryId, String locale) {
        return jpaRepository.findByCategoryIdAndLocale(categoryId, locale)
                .map(CategoryTranslationMapper::toDomain);
    }

    @Override
    public List<CategoryTranslation> findAllByCategoryId(UUID categoryId) {
        return jpaRepository.findByCategoryId(categoryId).stream()
                .map(CategoryTranslationMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByCategoryIdAndLocale(UUID categoryId, String locale) {
        CategoryTranslationId id = new CategoryTranslationId(categoryId, locale);
        if (jpaRepository.existsById(id)) {
            jpaRepository.deleteById(id);
        }
    }
}

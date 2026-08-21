package com.vointika.experience.domain.repository;

import com.vointika.experience.domain.entity.CategoryTranslation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryTranslationRepository {

    /** Create-or-replace the whole (category, locale) overlay row. */
    CategoryTranslation upsert(CategoryTranslation translation);

    Optional<CategoryTranslation> findByCategoryIdAndLocale(UUID categoryId, String locale);

    /** All translated locales for a category (one row each). */
    List<CategoryTranslation> findAllByCategoryId(UUID categoryId);

    /** Removes the (category, locale) row if present; idempotent. */
    void deleteByCategoryIdAndLocale(UUID categoryId, String locale);
}

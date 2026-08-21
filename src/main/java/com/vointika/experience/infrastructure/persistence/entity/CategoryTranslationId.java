package com.vointika.experience.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link CategoryTranslationJpaEntity}: (categoryId, locale). */
public class CategoryTranslationId implements Serializable {

    private UUID categoryId;
    private String locale;

    public CategoryTranslationId() {}

    public CategoryTranslationId(UUID categoryId, String locale) {
        this.categoryId = categoryId;
        this.locale = locale;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CategoryTranslationId other
                && Objects.equals(categoryId, other.categoryId)
                && Objects.equals(locale, other.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId, locale);
    }
}

package com.vointika.experience.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link ExperienceTranslationJpaEntity}: (experienceId, locale). */
public class ExperienceTranslationId implements Serializable {

    private UUID experienceId;
    private String locale;

    public ExperienceTranslationId() {}

    public ExperienceTranslationId(UUID experienceId, String locale) {
        this.experienceId = experienceId;
        this.locale = locale;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ExperienceTranslationId other
                && Objects.equals(experienceId, other.experienceId)
                && Objects.equals(locale, other.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(experienceId, locale);
    }
}

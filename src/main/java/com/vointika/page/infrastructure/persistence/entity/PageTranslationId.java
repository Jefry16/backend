package com.vointika.page.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite primary key for {@link PageTranslationJpaEntity}: (pageId, locale). */
public class PageTranslationId implements Serializable {

    private UUID pageId;
    private String locale;

    public PageTranslationId() {}

    public PageTranslationId(UUID pageId, String locale) {
        this.pageId = pageId;
        this.locale = locale;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PageTranslationId other
                && Objects.equals(pageId, other.pageId)
                && Objects.equals(locale, other.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, locale);
    }
}

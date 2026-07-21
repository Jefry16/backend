package com.vointika.reference.domain.entity;

import java.util.UUID;

/**
 * A content language the platform supports for storefront/experience content —
 * a curated reference lookup (sibling of {@link Currency}/{@link Timezone}).
 * The {@code code} is the BCP-47 locale (lowercased); operators enable a subset
 * of these, gated by {@code LanguageRepository.existsByCode} at the write
 * boundary (no FK). Distinct from the admin-UI language (a config allowlist).
 */
public class Language {

    private final UUID id;
    private final String code;
    private final String name;

    public Language(UUID id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}

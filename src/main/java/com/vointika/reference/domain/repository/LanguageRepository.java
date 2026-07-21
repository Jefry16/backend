package com.vointika.reference.domain.repository;

import com.vointika.reference.domain.entity.Language;

import java.util.List;

public interface LanguageRepository {
    List<Language> findAll();

    /** Whether a language with this (lowercased) code exists — the master-list gate. */
    boolean existsByCode(String code);
}

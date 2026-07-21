package com.vointika.reference.application.usecase;

import com.vointika.reference.domain.entity.Language;
import com.vointika.reference.domain.repository.LanguageRepository;

import java.util.List;

public class ListLanguagesUseCase {

    private final LanguageRepository languageRepository;

    public ListLanguagesUseCase(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    public List<Language> execute() {
        return languageRepository.findAll();
    }
}

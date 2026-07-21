package com.vointika.reference.infrastructure.persistence.repository;

import com.vointika.reference.domain.entity.Language;
import com.vointika.reference.domain.repository.LanguageRepository;
import com.vointika.reference.infrastructure.persistence.mapper.LanguageMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LanguageRepositoryImpl implements LanguageRepository {

    private final LanguageJpaRepository languageJpaRepository;

    public LanguageRepositoryImpl(LanguageJpaRepository languageJpaRepository) {
        this.languageJpaRepository = languageJpaRepository;
    }

    @Override
    public List<Language> findAll() {
        return languageJpaRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(LanguageMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return languageJpaRepository.existsByCode(code);
    }
}

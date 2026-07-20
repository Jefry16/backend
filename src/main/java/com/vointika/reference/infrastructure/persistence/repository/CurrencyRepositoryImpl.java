package com.vointika.reference.infrastructure.persistence.repository;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.infrastructure.persistence.mapper.CurrencyMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CurrencyRepositoryImpl implements CurrencyRepository {

    private final CurrencyJpaRepository currencyJpaRepository;

    public CurrencyRepositoryImpl(CurrencyJpaRepository currencyJpaRepository) {
        this.currencyJpaRepository = currencyJpaRepository;
    }

    @Override
    public List<Currency> findAll() {
        return currencyJpaRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(CurrencyMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Currency> findById(UUID id) {
        return currencyJpaRepository.findById(id).map(CurrencyMapper::toDomain);
    }
}

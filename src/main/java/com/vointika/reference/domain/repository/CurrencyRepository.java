package com.vointika.reference.domain.repository;

import com.vointika.reference.domain.entity.Currency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurrencyRepository {
    List<Currency> findAll();
    Optional<Currency> findById(UUID id);
}

package com.vointika.reference.domain.repository;

import com.vointika.reference.domain.entity.Country;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CountryRepository {
    List<Country> findAll();
    Optional<Country> findById(UUID id);
}

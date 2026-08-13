package com.vointika.reference.infrastructure.persistence.repository;

import com.vointika.reference.domain.entity.Country;
import com.vointika.reference.domain.repository.CountryRepository;
import com.vointika.reference.infrastructure.persistence.mapper.CountryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CountryRepositoryImpl implements CountryRepository {

    private final CountryJpaRepository countryJpaRepository;

    public CountryRepositoryImpl(CountryJpaRepository countryJpaRepository) {
        this.countryJpaRepository = countryJpaRepository;
    }

    @Override
    public List<Country> findAll() {
        return countryJpaRepository.findAllByOrderByNameAsc()
                .stream()
                .map(CountryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Country> findById(UUID id) {
        return countryJpaRepository.findById(id).map(CountryMapper::toDomain);
    }
}

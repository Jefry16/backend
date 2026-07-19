package com.vointika.reference.infrastructure.persistence.repository;

import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.reference.infrastructure.persistence.mapper.TimezoneMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TimezoneRepositoryImpl implements TimezoneRepository {

    private final TimezoneJpaRepository timezoneJpaRepository;

    public TimezoneRepositoryImpl(TimezoneJpaRepository timezoneJpaRepository) {
        this.timezoneJpaRepository = timezoneJpaRepository;
    }

    @Override
    public List<Timezone> findAll() {
        return timezoneJpaRepository.findAllWithCountryOrderByName()
                .stream()
                .map(TimezoneMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Timezone> findById(UUID id) {
        return timezoneJpaRepository.findById(id).map(TimezoneMapper::toDomain);
    }
}

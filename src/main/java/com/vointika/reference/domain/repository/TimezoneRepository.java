package com.vointika.reference.domain.repository;

import com.vointika.reference.domain.entity.Timezone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimezoneRepository {
    List<Timezone> findAll();
    Optional<Timezone> findById(UUID id);
}

package com.vointika.reference.infrastructure.persistence.repository;

import com.vointika.reference.infrastructure.persistence.entity.CountryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CountryJpaRepository extends JpaRepository<CountryJpaEntity, UUID> {
    List<CountryJpaEntity> findAllByOrderByNameAsc();
}

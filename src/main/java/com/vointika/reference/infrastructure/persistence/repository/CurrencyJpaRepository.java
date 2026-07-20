package com.vointika.reference.infrastructure.persistence.repository;

import com.vointika.reference.infrastructure.persistence.entity.CurrencyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CurrencyJpaRepository extends JpaRepository<CurrencyJpaEntity, UUID> {
    List<CurrencyJpaEntity> findAllByOrderByCodeAsc();
}

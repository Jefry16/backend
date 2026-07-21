package com.vointika.reference.infrastructure.persistence.repository;

import com.vointika.reference.infrastructure.persistence.entity.LanguageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LanguageJpaRepository extends JpaRepository<LanguageJpaEntity, UUID> {
    List<LanguageJpaEntity> findAllByOrderByCodeAsc();

    boolean existsByCode(String code);
}

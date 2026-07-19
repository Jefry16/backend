package com.vointika.reference.infrastructure.persistence.repository;

import com.vointika.reference.infrastructure.persistence.entity.TimezoneJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimezoneJpaRepository extends JpaRepository<TimezoneJpaEntity, UUID> {

    // JOIN FETCH the country so the list is one query, not N+1 (§3.5 / the batch principle).
    @Query("SELECT t FROM TimezoneJpaEntity t JOIN FETCH t.country ORDER BY t.name")
    List<TimezoneJpaEntity> findAllWithCountryOrderByName();

    // country is LAZY, but findById is called off the request/transaction-less path
    // (findCountryCode / findZoneId / findBrand) with open-in-view=false, and the mapper maps
    // country eagerly — so fetch it here or getCountry() hits a detached proxy
    // (LazyInitializationException). EntityGraph keeps the association LAZY-by-default elsewhere.
    @Override
    @EntityGraph(attributePaths = "country")
    Optional<TimezoneJpaEntity> findById(UUID id);
}

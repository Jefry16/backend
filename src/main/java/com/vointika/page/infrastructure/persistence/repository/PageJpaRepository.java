package com.vointika.page.infrastructure.persistence.repository;

import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PageJpaRepository extends JpaRepository<PageJpaEntity, UUID> {

    Optional<PageJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);

    boolean existsByTourOperatorIdAndHandleAndIdNot(UUID tourOperatorId, String handle, UUID excludeId);

    /** The storefront's link resolution: published pages only, many ids, one read. */
    List<PageJpaEntity> findByTourOperatorIdAndIdInAndPublishedTrue(
            UUID tourOperatorId, Collection<UUID> ids);

    /** The storefront addressing a page by its canonical handle. */
    Optional<PageJpaEntity> findByTourOperatorIdAndHandleAndPublishedTrue(
            UUID tourOperatorId, String handle);
}

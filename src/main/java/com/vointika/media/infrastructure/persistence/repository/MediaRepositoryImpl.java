package com.vointika.media.infrastructure.persistence.repository;

import com.vointika.media.application.usecase.ListMediaUseCase;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.media.infrastructure.persistence.entity.MediaJpaEntity;
import com.vointika.media.infrastructure.persistence.mapper.MediaMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class MediaRepositoryImpl implements MediaRepository {

    private final MediaJpaRepository jpaRepository;
    private final CriteriaListExecutor listExecutor;

    public MediaRepositoryImpl(MediaJpaRepository jpaRepository, CriteriaListExecutor listExecutor) {
        this.jpaRepository = jpaRepository;
        this.listExecutor = listExecutor;
    }

    @Override
    public Media save(Media media) {
        return MediaMapper.toDomain(jpaRepository.save(MediaMapper.toJpa(media)));
    }

    @Override
    public Optional<Media> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return jpaRepository.findByIdAndTourOperatorId(id, tourOperatorId).map(MediaMapper::toDomain);
    }

    @Override
    public CursorPage<Media> list(ListQuery query) {
        return listExecutor.list(
                MediaJpaEntity.class,
                ListMediaUseCase.SCHEMA,
                query,
                MediaMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean deleteByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        if (!jpaRepository.existsByIdAndTourOperatorId(id, tourOperatorId)) {
            return false;
        }
        jpaRepository.deleteByIdAndTourOperatorId(id, tourOperatorId);
        return true;
    }

    @Override
    public Map<UUID, MediaAsset> findAssetsByTourOperatorIdAndIds(UUID tourOperatorId, Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return jpaRepository.findAllByTourOperatorIdAndIdIn(tourOperatorId, ids).stream()
                .collect(Collectors.toMap(MediaJpaEntity::getId, MediaRepositoryImpl::toAsset));
    }

    private static MediaAsset toAsset(MediaJpaEntity media) {
        return new MediaAsset(media.getStorageKey(), media.getAlt(), media.getWidth(), media.getHeight());
    }
}

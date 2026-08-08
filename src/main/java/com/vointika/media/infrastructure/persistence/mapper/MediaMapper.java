package com.vointika.media.infrastructure.persistence.mapper;

import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.valueobject.MediaAlt;
import com.vointika.media.infrastructure.persistence.entity.MediaJpaEntity;

public class MediaMapper {

    public static MediaJpaEntity toJpa(Media media) {
        return new MediaJpaEntity(
                media.getId(),
                media.getTourOperatorId(),
                media.getStorageKey(),
                media.getContentType(),
                media.getSizeBytes(),
                media.getOriginalName(),
                media.getAlt() == null ? null : media.getAlt().value(),
                media.getWidth(),
                media.getHeight(),
                media.getCreatedBy(),
                media.getCreatedByName(),
                media.getCreatedAt());
    }

    public static Media toDomain(MediaJpaEntity jpa) {
        return new Media(
                jpa.getId(),
                jpa.getTourOperatorId(),
                jpa.getStorageKey(),
                jpa.getContentType(),
                jpa.getSizeBytes(),
                jpa.getOriginalName(),
                jpa.getCreatedBy(),
                jpa.getCreatedByName(),
                jpa.getCreatedAt(),
                jpa.getAlt() == null ? null : new MediaAlt(jpa.getAlt()),
                jpa.getWidth(),
                jpa.getHeight());
    }

    private MediaMapper() {}
}

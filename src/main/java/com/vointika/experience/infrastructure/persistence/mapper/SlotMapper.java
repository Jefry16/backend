package com.vointika.experience.infrastructure.persistence.mapper;

import com.vointika.experience.domain.entity.Slot;
import com.vointika.experience.infrastructure.persistence.entity.SlotJpaEntity;

public class SlotMapper {

    public static SlotJpaEntity toJpa(Slot s) {
        return new SlotJpaEntity(
                s.id(), s.experienceId(), s.tourOperatorId(),
                s.startAt(), s.endAt(), s.day(),
                s.experienceName(), s.experienceDescription(),
                s.status(), s.createdAt());
    }

    public static Slot toDomain(SlotJpaEntity j) {
        return new Slot(
                j.getId(), j.getExperienceId(), j.getTourOperatorId(),
                j.getStartAt(), j.getEndAt(),
                j.getExperienceName(), j.getExperienceDescription(),
                j.getStatus(), j.getCreatedAt());
    }

    private SlotMapper() {}
}

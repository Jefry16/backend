package com.vointika.experience.infrastructure.persistence.mapper;

import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.valueobject.BookingCutoffHours;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.experience.domain.valueobject.Price;
import com.vointika.shared.valueobject.SeoDescription;
import com.vointika.shared.valueobject.SeoTitle;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.shared.valueobject.Handle;

public class ExperienceMapper {

    public static ExperienceJpaEntity toJpa(Experience e) {
        return new ExperienceJpaEntity(
                e.getId(),
                e.getTourOperatorId(),
                e.getCreatedBy(),
                e.getHandle().value(),
                e.getName().value(),
                e.getDescription().value(),
                e.getLongDescription().value(),
                e.isFeatured(),
                e.getMediaIds(),
                e.getThumbnailMediaId(),
                e.getBookingCutoffHours().value(),
                e.isPublished(),
                e.getSeoTitle() == null ? null : e.getSeoTitle().value(),
                e.getSeoDescription() == null ? null : e.getSeoDescription().value(),
                e.getStartingPrice().value(),
                e.getCreatedAt());
    }

    public static Experience toDomain(ExperienceJpaEntity jpa) {
        return new Experience(
                jpa.getId(),
                jpa.getTourOperatorId(),
                jpa.getCreatedBy(),
                new Handle(jpa.getHandle()),
                jpa.getCreatedAt(),
                new ExperienceName(jpa.getName()),
                new Description(jpa.getDescription()),
                new LongDescription(jpa.getLongDescription()),
                jpa.isFeatured(),
                jpa.getMediaIds(),
                jpa.getThumbnailMediaId(),
                new BookingCutoffHours(jpa.getBookingCutoffHours()),
                jpa.isPublished(),
                jpa.getSeoTitle() == null ? null : new SeoTitle(jpa.getSeoTitle()),
                jpa.getSeoDescription() == null ? null : new SeoDescription(jpa.getSeoDescription()),
                new Price(jpa.getStartingPrice()));
    }

    private ExperienceMapper() {}
}

package com.vointika.experience.infrastructure.persistence.mapper;

import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.valueobject.BookingCutoffHours;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.DurationMinutes;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.Highlight;
import com.vointika.experience.domain.valueobject.InclusionItem;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.experience.domain.valueobject.Tag;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.shared.valueobject.Slug;

public class ExperienceMapper {

    public static ExperienceJpaEntity toJpa(Experience e) {
        return new ExperienceJpaEntity(
                e.getId(),
                e.getTourOperatorId(),
                e.getCreatedBy(),
                e.getSlug().value(),
                e.getName().value(),
                e.getDescription().value(),
                e.getLongDescription().value(),
                e.isFeatured(),
                e.getTags().stream().map(Tag::value).toList(),
                e.getIncluded().stream().map(InclusionItem::value).toList(),
                e.getNotIncluded().stream().map(InclusionItem::value).toList(),
                e.getHighlights().stream().map(Highlight::value).toList(),
                e.getMediaIds(),
                e.getThumbnailMediaId(),
                e.getDurationMinutes().value(),
                e.getBookingCutoffHours().value(),
                e.isPublished(),
                e.getCreatedAt());
    }

    public static Experience toDomain(ExperienceJpaEntity jpa) {
        return new Experience(
                jpa.getId(),
                jpa.getTourOperatorId(),
                jpa.getCreatedBy(),
                new Slug(jpa.getSlug()),
                jpa.getCreatedAt(),
                new ExperienceName(jpa.getName()),
                new Description(jpa.getDescription()),
                new LongDescription(jpa.getLongDescription()),
                jpa.isFeatured(),
                jpa.getTags().stream().map(Tag::new).toList(),
                jpa.getIncluded().stream().map(InclusionItem::new).toList(),
                jpa.getNotIncluded().stream().map(InclusionItem::new).toList(),
                jpa.getHighlights().stream().map(Highlight::new).toList(),
                jpa.getMediaIds(),
                jpa.getThumbnailMediaId(),
                new DurationMinutes(jpa.getDurationMinutes()),
                new BookingCutoffHours(jpa.getBookingCutoffHours()),
                jpa.isPublished());
    }

    private ExperienceMapper() {}
}

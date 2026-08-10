package com.vointika.experience.infrastructure.persistence.mapper;

import com.vointika.experience.domain.entity.ExperienceTranslation;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.InclusionItem;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.experience.domain.valueobject.SeoDescription;
import com.vointika.experience.domain.valueobject.SeoTitle;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;

import java.util.List;
import java.util.function.Function;

public class ExperienceTranslationMapper {

    public static ExperienceTranslationJpaEntity toJpa(ExperienceTranslation t) {
        return new ExperienceTranslationJpaEntity(
                t.experienceId(),
                t.locale().value(),
                t.tourOperatorId(),
                t.name() == null ? null : t.name().value(),
                t.description() == null ? null : t.description().value(),
                t.longDescription() == null ? null : t.longDescription().value(),
                strings(t.included(), InclusionItem::value),
                strings(t.notIncluded(), InclusionItem::value),
                t.handle() == null ? null : t.handle().value(),
                t.seoTitle() == null ? null : t.seoTitle().value(),
                t.seoDescription() == null ? null : t.seoDescription().value());
    }

    public static ExperienceTranslation toDomain(ExperienceTranslationJpaEntity jpa) {
        return new ExperienceTranslation(
                jpa.getExperienceId(),
                jpa.getTourOperatorId(),
                new LocaleCode(jpa.getLocale()),
                jpa.getName() == null ? null : new ExperienceName(jpa.getName()),
                jpa.getDescription() == null ? null : new Description(jpa.getDescription()),
                jpa.getLongDescription() == null ? null : new LongDescription(jpa.getLongDescription()),
                vos(jpa.getIncluded(), InclusionItem::new),
                vos(jpa.getNotIncluded(), InclusionItem::new),
                jpa.getHandle() == null ? null : new Handle(jpa.getHandle()),
                jpa.getSeoTitle() == null ? null : new SeoTitle(jpa.getSeoTitle()),
                jpa.getSeoDescription() == null ? null : new SeoDescription(jpa.getSeoDescription()));
    }

    private static <T> List<String> strings(List<T> vos, Function<T, String> value) {
        return vos == null ? null : vos.stream().map(value).toList();
    }

    private static <T> List<T> vos(List<String> raw, Function<String, T> vo) {
        return raw == null ? null : raw.stream().map(vo).toList();
    }

    private ExperienceTranslationMapper() {}
}

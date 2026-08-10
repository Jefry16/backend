package com.vointika.experience.application.service;

import com.vointika.experience.application.dto.input.ExperienceInput;
import com.vointika.experience.domain.valueobject.BookingCutoffHours;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.experience.domain.valueobject.Price;
import com.vointika.experience.domain.valueobject.SeoDescription;
import com.vointika.experience.domain.valueobject.SeoTitle;
import com.vointika.shared.exception.InvalidFieldException;

import java.util.List;
import java.util.UUID;

/**
 * Builds an experience's value objects from a raw {@link ExperienceInput}. Each
 * VO constructor validates (→ 422), so this is where field-level validation
 * happens for both the create and update paths. Null lists become empty.
 */
public final class ExperienceInputMapper {

    private ExperienceInputMapper() {}

    public static ExperienceName name(ExperienceInput in) { return new ExperienceName(in.name()); }
    public static Description description(ExperienceInput in) { return new Description(in.description()); }
    public static LongDescription longDescription(ExperienceInput in) { return new LongDescription(in.longDescription()); }



    public static List<UUID> mediaIds(ExperienceInput in) {
        return safe(in.mediaIds());
    }

    public static BookingCutoffHours bookingCutoffHours(ExperienceInput in) {
        if (in.bookingCutoffHours() == null) {
            throw new InvalidFieldException("Booking cutoff hours are required");
        }
        return new BookingCutoffHours(in.bookingCutoffHours());
    }

    /** Optional SEO overrides — blank or absent means "no override". */
    public static SeoTitle seoTitle(ExperienceInput in) {
        return blank(in.seoTitle()) ? null : new SeoTitle(in.seoTitle());
    }

    public static SeoDescription seoDescription(ExperienceInput in) {
        return blank(in.seoDescription()) ? null : new SeoDescription(in.seoDescription());
    }

    public static Price startingPrice(ExperienceInput in) {
        if (in.startingPrice() == null) {
            throw new InvalidFieldException("Starting price is required");
        }
        return new Price(in.startingPrice());
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : List.copyOf(list);
    }
}

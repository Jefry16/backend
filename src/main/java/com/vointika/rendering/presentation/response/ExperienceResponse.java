package com.vointika.rendering.presentation.response;

import com.vointika.shared.port.StorefrontExperienceView;

import java.util.List;

/**
 * An experience on the wire, already resolved for one locale.
 *
 * <p>Like {@link ShopResponse}, no {@code id}/{@code context} pair: this is a
 * render block for a theme, and the storefront addresses experiences by slug.
 */
public record ExperienceResponse(
        String slug,
        String name,
        String description,
        String longDescription,
        List<String> highlights,
        List<String> included,
        List<String> notIncluded,
        List<String> tags,
        String thumbnailUrl,
        List<String> mediaUrls,
        Integer durationMinutes,
        boolean featured) {

    public static ExperienceResponse from(StorefrontExperienceView experience) {
        return new ExperienceResponse(
                experience.slug(),
                experience.name(),
                experience.description(),
                experience.longDescription(),
                experience.highlights(),
                experience.included(),
                experience.notIncluded(),
                experience.tags(),
                experience.thumbnailUrl(),
                experience.mediaUrls(),
                experience.durationMinutes(),
                experience.featured());
    }
}

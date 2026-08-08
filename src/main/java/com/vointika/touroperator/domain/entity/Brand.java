package com.vointika.touroperator.domain.entity;

import com.vointika.touroperator.domain.enums.BrandColorRole;
import com.vointika.touroperator.domain.valueobject.BrandShortDescription;
import com.vointika.touroperator.domain.valueobject.BrandSlogan;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The operator's brand — Shopify's {@code shop.brand}, and the substrate the
 * storefront renders on every page.
 *
 * <p>One per operator, so the aggregate is keyed by {@code tourOperatorId} and
 * there is no id of its own: this is a settings object, not a collection member.
 *
 * <p><b>Every field is optional.</b> An operator who has filled in nothing still
 * has a brand — the storefront answers an empty one rather than null, and a
 * template guards on each piece. That is why this has no factory demanding
 * values and why {@link #empty} exists.
 *
 * <p>The two collections are <b>replaced wholesale</b> on write, the model
 * {@code PUT .../menus/{id}/items} uses: a palette is an ordered whole and
 * diffing positions against a partial payload is how orderings drift.
 *
 * <p>{@code slogan} and {@code shortDescription} are the canonical values here;
 * their per-locale overlays live on {@code tour_operator_translations} and are
 * written through the translations endpoint.
 */
public record Brand(
        UUID tourOperatorId,
        BrandSlogan slogan,
        BrandShortDescription shortDescription,
        UUID logoMediaId,
        UUID squareLogoMediaId,
        UUID faviconMediaId,
        UUID coverImageMediaId,
        List<BrandColor> colors,
        List<BrandSocialLink> socialLinks,
        Instant createdAt,
        Instant updatedAt) {

    public Brand {
        Objects.requireNonNull(tourOperatorId, "tourOperatorId");
        colors = colors == null ? List.of() : List.copyOf(colors);
        socialLinks = socialLinks == null ? List.of() : List.copyOf(socialLinks);
    }

    /** An operator who has filled in nothing — not an error, and not null. */
    public static Brand empty(UUID tourOperatorId) {
        return new Brand(tourOperatorId, null, null, null, null, null, null,
                List.of(), List.of(), null, null);
    }

    /** The palette for one role, in the order a theme indexes it. */
    public List<BrandColor> colorsOf(BrandColorRole role) {
        return colors.stream().filter(c -> c.role() == role).toList();
    }
}

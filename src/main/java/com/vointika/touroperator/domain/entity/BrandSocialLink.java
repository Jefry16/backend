package com.vointika.touroperator.domain.entity;

import com.vointika.touroperator.domain.enums.BrandSocialPlatform;
import com.vointika.touroperator.domain.valueobject.SocialUrl;

import java.util.Objects;

/**
 * One social profile. At most one per platform — the table's primary key is
 * {@code (tour_operator_id, platform)}, because a theme renders one icon per
 * platform and two Instagram links would have no way to choose.
 */
public record BrandSocialLink(BrandSocialPlatform platform, SocialUrl url) {

    public BrandSocialLink {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(url, "url");
    }
}

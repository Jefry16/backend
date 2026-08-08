package com.vointika.touroperator.domain.entity;

import com.vointika.touroperator.domain.enums.BrandColorRole;
import com.vointika.touroperator.domain.valueobject.HexColor;

import java.util.Objects;

/**
 * One entry in the brand palette. {@code position} is a promise a theme indexes
 * into — {@code colors.primary[0].background} is a real address in the published
 * storefront contract — so the order is data, not a rendering accident.
 */
public record BrandColor(BrandColorRole role, int position, HexColor background, HexColor foreground) {

    public BrandColor {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(background, "background");
        Objects.requireNonNull(foreground, "foreground");
    }
}

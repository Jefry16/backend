package com.vointika.rendering.application.dto.output;

import com.vointika.shared.port.StorefrontExperienceView;
import com.vointika.shared.port.StorefrontOperatorView;

/** One experience's own page. */
public record ExperienceRenderContext(
        StorefrontOperatorView shop,
        String locale,
        StorefrontExperienceView experience,
        java.util.List<NavigationMenu> navigation) {}

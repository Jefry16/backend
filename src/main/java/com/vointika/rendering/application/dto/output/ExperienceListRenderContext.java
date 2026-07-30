package com.vointika.rendering.application.dto.output;

import com.vointika.shared.port.StorefrontExperienceView;
import com.vointika.shared.port.StorefrontOperatorView;

import java.util.List;

/** The experience-list page: the tenant's chrome plus everything it sells. */
public record ExperienceListRenderContext(
        StorefrontOperatorView shop,
        String locale,
        List<StorefrontExperienceView> experiences) {}

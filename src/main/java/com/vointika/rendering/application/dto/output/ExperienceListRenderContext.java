package com.vointika.rendering.application.dto.output;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.port.StorefrontExperienceView;
import com.vointika.shared.port.StorefrontOperatorView;

/** The experience-list page: the tenant's chrome plus one page of what it sells. */
public record ExperienceListRenderContext(
        StorefrontOperatorView shop,
        String locale,
        CursorPage<StorefrontExperienceView> experiences,
        java.util.List<NavigationMenu> navigation) {}

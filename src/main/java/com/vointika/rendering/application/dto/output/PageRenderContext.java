package com.vointika.rendering.application.dto.output;

import com.vointika.shared.port.StorefrontOperatorView;
import com.vointika.shared.port.StorefrontPageView;

/** One CMS page. */
public record PageRenderContext(
        StorefrontOperatorView shop,
        String locale,
        StorefrontPageView page,
        java.util.List<NavigationMenu> navigation) {}

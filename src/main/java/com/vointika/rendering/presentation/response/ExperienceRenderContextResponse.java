package com.vointika.rendering.presentation.response;

import com.vointika.rendering.application.dto.output.ExperienceRenderContext;

/** `{shop, request, experience}` — one experience's page. */
public record ExperienceRenderContextResponse(
        ShopResponse shop,
        RequestResponse request,
        ExperienceResponse experience,
        java.util.List<NavigationMenuResponse> navigation) {

    public static ExperienceRenderContextResponse from(ExperienceRenderContext context) {
        return new ExperienceRenderContextResponse(
                ShopResponse.from(context.shop()),
                new RequestResponse(context.locale()),
                ExperienceResponse.from(context.experience()),
                context.navigation().stream().map(NavigationMenuResponse::from).toList());
    }
}

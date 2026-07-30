package com.vointika.rendering.presentation.response;

import com.vointika.rendering.application.dto.output.ExperienceListRenderContext;

import java.util.List;

/** `{shop, request, experiences}` — the same two chrome blocks, plus content. */
public record ExperienceListRenderContextResponse(
        ShopResponse shop,
        RequestResponse request,
        List<ExperienceResponse> experiences) {

    public static ExperienceListRenderContextResponse from(ExperienceListRenderContext context) {
        return new ExperienceListRenderContextResponse(
                ShopResponse.from(context.shop()),
                new RequestResponse(context.locale()),
                context.experiences().stream().map(ExperienceResponse::from).toList());
    }
}

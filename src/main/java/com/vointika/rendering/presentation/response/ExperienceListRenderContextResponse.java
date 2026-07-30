package com.vointika.rendering.presentation.response;

import com.vointika.rendering.application.dto.output.ExperienceListRenderContext;

import java.util.List;

/**
 * `{shop, request, experiences, nextCursor}` — the two chrome blocks plus one
 * page of content.
 *
 * <p>The page is flattened rather than nested as the admin API's
 * `{data, nextCursor}` envelope: a theme iterates `experiences` directly, and
 * one less level of indirection in a template is worth more here than symmetry
 * with an API no theme author will ever read. {@code nextCursor} is null on the
 * last page.
 */
public record ExperienceListRenderContextResponse(
        ShopResponse shop,
        RequestResponse request,
        List<ExperienceResponse> experiences,
        String nextCursor,
        List<NavigationMenuResponse> navigation) {

    public static ExperienceListRenderContextResponse from(ExperienceListRenderContext context) {
        return new ExperienceListRenderContextResponse(
                ShopResponse.from(context.shop()),
                new RequestResponse(context.locale()),
                context.experiences().data().stream().map(ExperienceResponse::from).toList(),
                context.experiences().nextCursor(),
                context.navigation().stream().map(NavigationMenuResponse::from).toList());
    }
}

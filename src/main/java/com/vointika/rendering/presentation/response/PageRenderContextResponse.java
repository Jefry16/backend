package com.vointika.rendering.presentation.response;

import com.vointika.rendering.application.dto.output.PageRenderContext;

/** `{shop, request, page}` — one CMS page. */
public record PageRenderContextResponse(
        ShopResponse shop,
        RequestResponse request,
        PageResponse page,
        java.util.List<NavigationMenuResponse> navigation) {

    public static PageRenderContextResponse from(PageRenderContext context) {
        return new PageRenderContextResponse(
                ShopResponse.from(context.shop()),
                new RequestResponse(context.locale()),
                PageResponse.from(context.page()),
                context.navigation().stream().map(NavigationMenuResponse::from).toList());
    }
}

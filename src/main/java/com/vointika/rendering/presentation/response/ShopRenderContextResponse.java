package com.vointika.rendering.presentation.response;

import com.vointika.rendering.application.dto.output.ShopRenderContext;

/**
 * The chrome-only render context on the wire: {@code {shop, request}}.
 *
 * <p>Every page-type render context added later carries these same two blocks
 * plus its own content, so a theme can rely on their shape everywhere.
 */
public record ShopRenderContextResponse(ShopResponse shop, RequestResponse request) {

    public static ShopRenderContextResponse from(ShopRenderContext context) {
        return new ShopRenderContextResponse(
                ShopResponse.from(context.shop()),
                new RequestResponse(context.locale()));
    }
}

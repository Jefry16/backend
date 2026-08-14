package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetStorefrontGlobalsUseCase;
import com.vointika.storefront.presentation.response.StorefrontGlobalsResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * The home page, which for now is the globals and nothing else — the same shape
 * Shopify's index template gets, since that template has no object of its own.
 *
 * <p><b>Two mappings rather than one with an optional path variable.</b> The
 * bare path and the locale-prefixed one are separate patterns, and a
 * {@code @PathVariable} that exists in only one of them is a behaviour worth not
 * depending on when two three-line methods say it plainly.
 *
 * <p><b>Every media reference in the response is resolved in one call</b>, which
 * is why {@link StorefrontGlobalsResponse#mediaIds} exists: brand images and the
 * page image are four or five ids, and resolving them one at a time is four or
 * five round trips per page. Same rule as the experience galleries.
 *
 * <p><b>Both failures are one 404</b>: a handle no operator owns and a locale the
 * operator does not publish answer identically, so an anonymous visitor learns
 * nothing about which operators exist or which languages they have.
 */
@RestController
public class StorefrontHomeController {

    private final TenantHandleResolver tenantHandleResolver;
    private final GetStorefrontGlobalsUseCase getStorefrontGlobals;
    private final MediaAssetBatchQuery mediaAssetBatchQuery;
    private final MediaUrlResolver mediaUrlResolver;

    public StorefrontHomeController(TenantHandleResolver tenantHandleResolver,
                                    GetStorefrontGlobalsUseCase getStorefrontGlobals,
                                    MediaAssetBatchQuery mediaAssetBatchQuery,
                                    MediaUrlResolver mediaUrlResolver) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.getStorefrontGlobals = getStorefrontGlobals;
        this.mediaAssetBatchQuery = mediaAssetBatchQuery;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @GetMapping(path = StorefrontRoutes.HOME, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse home(HttpServletRequest request) {
        return render(request, null);
    }

    @GetMapping(path = StorefrontRoutes.LOCALE, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse localizedHome(@PathVariable String locale, HttpServletRequest request) {
        return render(request, locale);
    }

    private StorefrontGlobalsResponse render(HttpServletRequest request, String pathLocale) {
        String handle = tenantHandleResolver.resolve(request.getServerName())
                .orElseThrow(StorefrontControllers::notFound);
        StorefrontGlobals globals = getStorefrontGlobals.execute(handle, pathLocale)
                .orElseThrow(StorefrontControllers::notFound);

        Map<UUID, MediaAsset> assets = StorefrontControllers.assets(globals, mediaAssetBatchQuery);

        return StorefrontGlobalsResponse.from(globals, StorefrontControllers.origin(request), assets, mediaUrlResolver);
    }

}

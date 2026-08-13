package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.storefront.application.dto.output.StorefrontPageOutput;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetStorefrontCmsPageUseCase;
import com.vointika.storefront.presentation.response.StorefrontGlobalsResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A CMS page — the second route with an object of its own, and the first one a
 * menu item has been able to link to since menus landed.
 *
 * <p><b>Every miss is the same 404</b>: an unknown operator, a locale it does not
 * publish, a handle nothing answers to, a draft, and the canonical handle of a
 * page this locale renames. Telling any of them apart would leak what an
 * operator has not published.
 */
@RestController
public class StorefrontCmsPageController {

    private final TenantHandleResolver tenantHandleResolver;
    private final GetStorefrontCmsPageUseCase getStorefrontCmsPage;
    private final MediaAssetBatchQuery mediaAssetBatchQuery;
    private final MediaUrlResolver mediaUrlResolver;

    public StorefrontCmsPageController(TenantHandleResolver tenantHandleResolver,
                                       GetStorefrontCmsPageUseCase getStorefrontCmsPage,
                                       MediaAssetBatchQuery mediaAssetBatchQuery,
                                       MediaUrlResolver mediaUrlResolver) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.getStorefrontCmsPage = getStorefrontCmsPage;
        this.mediaAssetBatchQuery = mediaAssetBatchQuery;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @GetMapping(path = StorefrontRoutes.PAGE, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse page(@PathVariable String handle, HttpServletRequest request) {
        return render(request, null, handle);
    }

    @GetMapping(path = StorefrontRoutes.LOCALIZED_PAGE, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse localizedPage(@PathVariable String locale,
                                                   @PathVariable String handle,
                                                   HttpServletRequest request) {
        return render(request, locale, handle);
    }

    private StorefrontGlobalsResponse render(HttpServletRequest request, String pathLocale, String handle) {
        String shopHandle = tenantHandleResolver.resolve(request.getServerName())
                .orElseThrow(StorefrontControllers::notFound);
        StorefrontPageOutput output = getStorefrontCmsPage.execute(shopHandle, pathLocale, handle)
                .orElseThrow(StorefrontControllers::notFound);

        Set<UUID> mediaIds = StorefrontGlobalsResponse.mediaIds(output.globals());
        Map<UUID, MediaAsset> assets = mediaIds.isEmpty()
                ? Map.of()
                : mediaAssetBatchQuery.findAssetsByIds(output.globals().tourOperator().id(), mediaIds);

        return StorefrontGlobalsResponse.from(output.globals(), output.page(),
                StorefrontControllers.origin(request), assets, mediaUrlResolver);
    }
}

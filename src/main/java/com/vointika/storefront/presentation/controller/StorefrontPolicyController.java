package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.storefront.application.dto.output.StorefrontPolicyOutput;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetStorefrontPolicyUseCase;
import com.vointika.storefront.presentation.response.StorefrontGlobalsResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * One policy's page — the address {@code tourOperator.policies[].url} and the
 * named accessors beside it have pointed at since the globals shipped.
 *
 * <p><b>The slug is not a handle.</b> It is derived from a closed enum rather
 * than typed by an operator, so it does not vary by locale: {@code /es/policies/
 * terms} is the Spanish page at the English-looking slug, and there is no
 * localized-slug namespace to resolve against. That is why this route needs no
 * {@code LocalizedHandles} where the page and experience routes do.
 *
 * <p><b>Every miss is the same 404</b>: an unknown host, a locale the operator
 * does not publish, a slug no policy type has, and a type this operator never
 * wrote. The last two matter — telling them apart would let an anonymous visitor
 * enumerate which of the four policies an operator has.
 */
@RestController
public class StorefrontPolicyController {

    private final TenantHandleResolver tenantHandleResolver;
    private final GetStorefrontPolicyUseCase getStorefrontPolicy;
    private final MediaAssetBatchQuery mediaAssetBatchQuery;
    private final MediaUrlResolver mediaUrlResolver;

    public StorefrontPolicyController(TenantHandleResolver tenantHandleResolver,
                                      GetStorefrontPolicyUseCase getStorefrontPolicy,
                                      MediaAssetBatchQuery mediaAssetBatchQuery,
                                      MediaUrlResolver mediaUrlResolver) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.getStorefrontPolicy = getStorefrontPolicy;
        this.mediaAssetBatchQuery = mediaAssetBatchQuery;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @GetMapping(path = StorefrontRoutes.POLICY, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse policy(@PathVariable String type, HttpServletRequest request) {
        return render(request, null, type);
    }

    @GetMapping(path = StorefrontRoutes.LOCALIZED_POLICY, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse localizedPolicy(@PathVariable String locale,
                                                     @PathVariable String type,
                                                     HttpServletRequest request) {
        return render(request, locale, type);
    }

    private StorefrontGlobalsResponse render(HttpServletRequest request, String pathLocale, String slug) {
        String operatorHandle = tenantHandleResolver.resolve(request.getServerName())
                .orElseThrow(StorefrontControllers::notFound);

        StorefrontPolicyOutput output = getStorefrontPolicy.execute(operatorHandle, pathLocale, slug)
                .orElseThrow(StorefrontControllers::notFound);

        Map<UUID, MediaAsset> assets = StorefrontControllers.assets(output.globals(), mediaAssetBatchQuery);

        return StorefrontGlobalsResponse.policy(output.globals(), output.policy(),
                StorefrontControllers.origin(request), assets, mediaUrlResolver);
    }
}

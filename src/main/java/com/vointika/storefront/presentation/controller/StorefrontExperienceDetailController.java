package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.storefront.application.dto.output.StorefrontExperienceOutput;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetStorefrontExperienceUseCase;
import com.vointika.storefront.presentation.response.StorefrontGlobalsResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * One experience's page — the address the listing's cards and every
 * {@code EXPERIENCE} menu link have pointed at since before it existed.
 *
 * <p><b>Every miss is the same 404</b>: an unknown operator, a locale it does not
 * publish, a handle nothing answers to, a draft, and the canonical handle of an
 * experience this locale renames. A visitor learns nothing about which of the
 * five it hit, which is the point — the alternative tells an anonymous caller
 * which handles exist and which locales an operator has.
 *
 * <p><b>Named {@code …DetailController} beside {@code …ListController}</b> rather
 * than taking the bare noun. Two classes with one simple name are one bean name
 * whatever their packages, and the collision is a startup failure only
 * {@code contextLoads} catches (PATTERNS §11); more to the point, "experience
 * controller" would not say which of the two addresses it serves.
 */
@RestController
public class StorefrontExperienceDetailController {

    private final TenantHandleResolver tenantHandleResolver;
    private final GetStorefrontExperienceUseCase getStorefrontExperience;
    private final MediaAssetBatchQuery mediaAssetBatchQuery;
    private final MediaUrlResolver mediaUrlResolver;

    public StorefrontExperienceDetailController(TenantHandleResolver tenantHandleResolver,
                                                GetStorefrontExperienceUseCase getStorefrontExperience,
                                                MediaAssetBatchQuery mediaAssetBatchQuery,
                                                MediaUrlResolver mediaUrlResolver) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.getStorefrontExperience = getStorefrontExperience;
        this.mediaAssetBatchQuery = mediaAssetBatchQuery;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @GetMapping(path = StorefrontRoutes.EXPERIENCE, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse experience(@PathVariable String handle, HttpServletRequest request) {
        return render(request, null, handle);
    }

    @GetMapping(path = StorefrontRoutes.LOCALIZED_EXPERIENCE, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse localizedExperience(@PathVariable String locale,
                                                         @PathVariable String handle,
                                                         HttpServletRequest request) {
        return render(request, locale, handle);
    }

    private StorefrontGlobalsResponse render(HttpServletRequest request, String pathLocale, String handle) {
        String operatorHandle = tenantHandleResolver.resolve(request.getServerName())
                .orElseThrow(StorefrontControllers::notFound);

        StorefrontExperienceOutput output = getStorefrontExperience
                .execute(operatorHandle, pathLocale, handle)
                .orElseThrow(StorefrontControllers::notFound);

        // The gallery joins the globals' media in one batch; resolved separately
        // it would be one lookup per photo, up to twenty of them.
        Map<UUID, MediaAsset> assets =
                StorefrontControllers.assets(output.globals(), output.experience(), mediaAssetBatchQuery);

        return StorefrontGlobalsResponse.experience(output.globals(), output.experience(),
                output.metafields(), StorefrontControllers.origin(request), assets, mediaUrlResolver);
    }
}

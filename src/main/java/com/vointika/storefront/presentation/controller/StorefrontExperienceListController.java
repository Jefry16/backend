package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.storefront.application.dto.output.StorefrontExperienceListOutput;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetStorefrontExperienceListUseCase;
import com.vointika.storefront.presentation.response.StorefrontGlobalsResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * The experiences listing. <b>It serves the globals and nothing else today</b> —
 * the same body as the home page, differing only in {@code pageType}
 * ({@code list-experiences}) and {@code canonicalUrl}.
 *
 * <p><b>That is the point of shipping it in this shape.</b> The listing's own
 * query is a separate piece of work; establishing the address on the real render
 * path first means the thing that lands next is a field on a payload that already
 * works, rather than a route, a locale rule, a gate interaction and a query all
 * at once. It left {@code StorefrontPlaceholderController} for the same reason
 * the home page did.
 *
 * <p><b>The render is deliberately a copy of the home controller's, and it is
 * about to stop being one.</b> The listing query arrives here and nowhere else,
 * so extracting the shared eight lines now would be undone by the next slice —
 * §4c's rule for DTOs applied to a method: a copy that is about to diverge is not
 * the same thing as a copy that insulates nothing. What the two genuinely share
 * already lives in {@link StorefrontControllers}.
 *
 * <p><b>Both failures are one 404</b>: a handle no operator owns and a locale the
 * operator does not publish answer identically, so an anonymous visitor learns
 * nothing about which operators exist or which languages they have.
 */
@RestController
public class StorefrontExperienceListController {

    private final TenantHandleResolver tenantHandleResolver;
    private final GetStorefrontExperienceListUseCase getStorefrontExperienceList;
    private final MediaAssetBatchQuery mediaAssetBatchQuery;
    private final MediaUrlResolver mediaUrlResolver;

    public StorefrontExperienceListController(TenantHandleResolver tenantHandleResolver,
                                              GetStorefrontExperienceListUseCase getStorefrontExperienceList,
                                              MediaAssetBatchQuery mediaAssetBatchQuery,
                                              MediaUrlResolver mediaUrlResolver) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.getStorefrontExperienceList = getStorefrontExperienceList;
        this.mediaAssetBatchQuery = mediaAssetBatchQuery;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    /**
     * The listing reads {@code cursor} and ignores every other parameter.
     *
     * <p><b>Not {@code ListQueryParser}, and this is the one place the storefront
     * cannot reuse the admin's list plumbing.</b> That parser answers <b>422</b> to
     * any parameter it does not recognise (#134), which is right for an API and
     * wrong for a public page: `?utm_source=`, `fbclid`, `gclid` and every other
     * tracking parameter ride on links we do not control, so a strict parser turns
     * a newsletter click into an error page. It was caught by
     * {@code theCanonicalUrlDropsTheQueryString} failing — the canonical tag exists
     * precisely because those parameters arrive.
     *
     * <p>Reading one parameter is also stronger than validating many. No caller
     * input reaches {@code filters} at all, so {@code ?filter[published][eq]=false}
     * cannot serve drafts however the schema is later edited — the schema-based
     * refusal is a rule, this is an absence.
     *
     * <p>The consequence to accept: an unsupported parameter is ignored rather than
     * refused, so a theme author who invents `?sort=name` gets the default order
     * and no complaint. The day the listing takes real filters, they are read here
     * by name and passed as arguments — never by handing the whole query string to
     * the strict parser.
     */
    private static String cursorOf(HttpServletRequest request) {
        return request.getParameter("cursor");
    }

    @GetMapping(path = StorefrontRoutes.EXPERIENCES, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse experiences(HttpServletRequest request) {
        return render(request, null);
    }

    @GetMapping(path = StorefrontRoutes.LOCALIZED_EXPERIENCES, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontGlobalsResponse localizedExperiences(@PathVariable String locale,
                                                          HttpServletRequest request) {
        return render(request, locale);
    }

    private StorefrontGlobalsResponse render(HttpServletRequest request, String pathLocale) {
        String handle = tenantHandleResolver.resolve(request.getServerName())
                .orElseThrow(StorefrontControllers::notFound);

        // The tenant is null here on purpose: the adapter scopes to the operator
        // the host resolved to, so a visitor never names one.
        StorefrontExperienceListOutput output = getStorefrontExperienceList
                .execute(handle, pathLocale, cursorOf(request))
                .orElseThrow(StorefrontControllers::notFound);

        Map<UUID, MediaAsset> assets = StorefrontControllers.assets(output.globals(), mediaAssetBatchQuery);

        return StorefrontGlobalsResponse.experienceList(output.globals(), output.experiences(),
                StorefrontControllers.origin(request), assets, mediaUrlResolver);
    }
}

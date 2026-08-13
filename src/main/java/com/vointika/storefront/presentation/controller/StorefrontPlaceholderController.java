package com.vointika.storefront.presentation.controller;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontTenantUseCase;
import com.vointika.storefront.presentation.response.StorefrontPlaceholderResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The storefront addresses that do not have a page yet: experiences and
 * policies, bare and locale-prefixed. A real tenant gets a JSON placeholder and
 * anything else gets a 404.
 *
 * <p><b>The home page left this controller</b> when the globals landed — see
 * {@code StorefrontHomeController}. What is still here answers the tenant
 * question and nothing more, and each route leaves as its own page arrives.
 *
 * <p><b>Tenant resolution is the part that survived the cutback</b>, and it is
 * the reason this is not a static file. The host still names a tenant
 * ({@link TenantHandleResolver}: apex, multi-label and reserved labels address
 * nobody), and an unknown handle still 404s, so the addresses keep telling the
 * truth about which storefronts exist. Everything that read a storefront's
 * <em>data</em> — the operator, its brand, its policies, its published experiences,
 * the theme object model and the password gate — was deleted rather than left
 * running against a page that renders none of it. Recover it from git when the
 * real pages return.
 *
 * <p><b>JSON and not HTML, until themes land.</b> The two placeholder templates
 * are gone; the engine under them is not. Answering JSON keeps the data honest
 * while the pages are absent — a wrong field shows up as a wrong field rather
 * than as markup nobody is reading yet — and the render-path decision (MAP open
 * decision 6) still stands, so {@code StorefrontMustacheConfig} keeps the
 * compiler and its settings ready for the themes.
 *
 * <p>Every route answers the same body. They are kept because the addresses are
 * the expensive part to get right again — see {@code StorefrontPublicRoutes} for
 * what each registry costs when one is missed — not because a placeholder needs
 * six of them.
 *
 * <p><b>HEAD is not mapped here and does not need to be:</b> Spring MVC serves
 * it from a {@code @GetMapping} for free. What it <em>does</em> need is its own
 * {@code PublicRoute} entry, because Spring Security matches the exact method
 * and rejects an unlisted one at the filter chain as a 401 before MVC is
 * reached.
 */
@RestController
public class StorefrontPlaceholderController {

    private final TenantHandleResolver tenantHandleResolver;
    private final CheckStorefrontTenantUseCase checkStorefrontTenant;

    public StorefrontPlaceholderController(TenantHandleResolver tenantHandleResolver,
                                           CheckStorefrontTenantUseCase checkStorefrontTenant) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.checkStorefrontTenant = checkStorefrontTenant;
    }

    /**
     * The 404 is thrown rather than built, so an address no storefront answers on
     * returns the application's one error shape instead of a second one invented
     * for two pages.
     */
    @GetMapping(path = {
            StorefrontRoutes.EXPERIENCES,
            StorefrontRoutes.LOCALIZED_EXPERIENCES,
            StorefrontRoutes.POLICY,
            StorefrontRoutes.LOCALIZED_POLICY
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public StorefrontPlaceholderResponse placeholder(HttpServletRequest request) {
        String handle = tenantHandleResolver.resolve(request.getServerName())
                .filter(checkStorefrontTenant::execute)
                .orElseThrow(() -> new ResourceNotFoundException("There is no storefront at this address"));

        return new StorefrontPlaceholderResponse(handle);
    }
}

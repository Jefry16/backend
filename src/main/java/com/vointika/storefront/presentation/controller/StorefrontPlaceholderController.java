package com.vointika.storefront.presentation.controller;

import com.samskivert.mustache.Template;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontTenantUseCase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The whole storefront, for now: a real tenant gets a placeholder page and
 * anything else gets a 404.
 *
 * <p><b>Tenant resolution is the part that survived the cutback</b>, and it is
 * the reason this is not a static file. The host still names a tenant
 * ({@link TenantHandleResolver}: apex, multi-label and reserved labels address
 * nobody), and an unknown handle still 404s, so the addresses keep telling the
 * truth about which storefronts exist. Everything that read a storefront's
 * <em>data</em> — the shop, its brand, its policies, its published experiences,
 * the theme object model and the password gate — was deleted rather than left
 * running against a page that renders none of it. Recover it from git when the
 * real pages return.
 *
 * <p>Every route renders the same page. They are kept because the addresses are
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

    private static final MediaType HTML_UTF8 = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    private final TenantHandleResolver tenantHandleResolver;
    private final CheckStorefrontTenantUseCase checkStorefrontTenant;
    private final Template placeholderTemplate;
    private final Template notFoundTemplate;

    public StorefrontPlaceholderController(TenantHandleResolver tenantHandleResolver,
                                           CheckStorefrontTenantUseCase checkStorefrontTenant,
                                           Template storefrontPlaceholderTemplate,
                                           Template storefrontNotFoundTemplate) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.checkStorefrontTenant = checkStorefrontTenant;
        this.placeholderTemplate = storefrontPlaceholderTemplate;
        this.notFoundTemplate = storefrontNotFoundTemplate;
    }

    @GetMapping(path = {
            StorefrontRoutes.HOME,
            StorefrontRoutes.LOCALE,
            StorefrontRoutes.EXPERIENCES,
            StorefrontRoutes.LOCALIZED_EXPERIENCES,
            StorefrontRoutes.POLICY,
            StorefrontRoutes.LOCALIZED_POLICY
    }, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> placeholder(HttpServletRequest request) {
        boolean known = tenantHandleResolver.resolve(request.getServerName())
                .filter(checkStorefrontTenant::execute)
                .isPresent();

        return known
                ? ResponseEntity.ok().contentType(HTML_UTF8).body(placeholderTemplate.execute(Map.of()))
                : ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(HTML_UTF8)
                        .body(notFoundTemplate.execute(Map.of()));
    }
}

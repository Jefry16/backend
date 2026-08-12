package com.vointika.storefront.infrastructure.web;

import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the gate on <b>exactly the storefront's page routes</b> — never
 * {@code /**}, which would front {@code /api/**} and hand the admin API a
 * redirect to a storefront page, and never {@code /*}, which is wider than the
 * storefront in the same way a bare {@code /{locale}} would be.
 *
 * <p>Two paths fall outside by construction, and both are deliberate.
 * {@link StorefrontRoutes#PASSWORD} is where the redirect points, so gating it
 * would loop. The container's {@code /error} dispatch stays ungated so a genuine
 * 500 on a locked store is still a 500 — uniform refusal is about the
 * storefront's own pages, not about rewriting the servlet container's error
 * handling.
 *
 * <p><b>A page route added later has to be added here too</b>, which is the
 * fourth registry a storefront route costs on top of the {@code @GetMapping} and
 * the two {@code PublicRoute} entries. There is no pattern broad enough to catch
 * one for free, and that is the point: the broad pattern is the bug.
 */
@Configuration("storefrontWebConfig")
public class StorefrontWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<TenantHandleResolver> tenantHandleResolver;
    private final ObjectProvider<CheckStorefrontLockUseCase> checkStorefrontLock;

    public StorefrontWebConfig(ObjectProvider<TenantHandleResolver> tenantHandleResolver,
                               ObjectProvider<CheckStorefrontLockUseCase> checkStorefrontLock) {
        this.tenantHandleResolver = tenantHandleResolver;
        this.checkStorefrontLock = checkStorefrontLock;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new StorefrontLockInterceptor(tenantHandleResolver, checkStorefrontLock))
                .addPathPatterns(StorefrontRoutes.HOME, StorefrontRoutes.LOCALE,
                        StorefrontRoutes.EXPERIENCES, StorefrontRoutes.LOCALIZED_EXPERIENCES,
                        StorefrontRoutes.POLICY, StorefrontRoutes.LOCALIZED_POLICY,
                        StorefrontRoutes.PAGE, StorefrontRoutes.LOCALIZED_PAGE);
    }
}

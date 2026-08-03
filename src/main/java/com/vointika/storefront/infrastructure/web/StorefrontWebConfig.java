package com.vointika.storefront.infrastructure.web;

import com.vointika.storefront.application.policy.LocaleResolver;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the gate on <b>exactly the storefront's page routes</b> — never
 * {@code /**}, which would front {@code /api/**} and hand the admin API a
 * redirect to a storefront page, and no longer {@code /*}, which was wider than
 * the storefront in the same way a bare {@code /{locale}} was.
 *
 * <p>Two paths fall outside by construction, and both are deliberate.
 * {@code /password} is where the redirect points, so gating it would loop.
 * The container's {@code /error} dispatch stays ungated so a genuine 500 on a
 * locked store is still a 500 — uniform refusal is about the storefront's own
 * pages, not about rewriting the servlet container's error handling.
 *
 * <p>A page route added later has to be added here too; there is no pattern
 * broad enough to catch one for free, and that is the point.
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
                .addPathPatterns("/", LocaleResolver.PATH_TEMPLATE);
    }
}

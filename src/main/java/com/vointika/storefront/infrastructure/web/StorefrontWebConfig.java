package com.vointika.storefront.infrastructure.web;

import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the gate on the storefront's own patterns — <b>never {@code /**}</b>,
 * which would put it in front of {@code /api/**} and hand the admin API a
 * redirect to a storefront page. Every storefront route is one segment or fewer
 * today, so {@code /} and {@code /*} are exactly the surface; a deeper route
 * added later has to be added here too.
 *
 * <p>{@code /password} is excluded, or the page the redirect points at would
 * redirect to itself.
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
                .addPathPatterns("/", "/*")
                .excludePathPatterns("/password");
    }
}

package com.vointika.storefront.infrastructure.config;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.port.UnlockTokenPort;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontTenantUseCase;
import com.vointika.storefront.application.usecase.GetPasswordPageUseCase;
import com.vointika.storefront.application.usecase.GetStorefrontGlobalsUseCase;
import com.vointika.storefront.application.usecase.UnlockStorefrontUseCase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Two beans, which is the whole context while the storefront is a placeholder.
 * Use cases are plain POJOs hand-wired here, as everywhere.
 */
@Configuration("storefrontUseCaseConfig")
@EnableConfigurationProperties(StorefrontProperties.class)
public class StorefrontUseCaseConfig {

    @Bean
    public CheckStorefrontTenantUseCase checkStorefrontTenantUseCase(
            StorefrontShopQuery storefrontShopQuery) {
        return new CheckStorefrontTenantUseCase(storefrontShopQuery);
    }

    @Bean
    public GetStorefrontGlobalsUseCase getStorefrontGlobalsUseCase(
            StorefrontShopQuery storefrontShopQuery) {
        return new GetStorefrontGlobalsUseCase(storefrontShopQuery);
    }

    @Bean
    public CheckStorefrontLockUseCase checkStorefrontLockUseCase(
            StorefrontShopQuery storefrontShopQuery, UnlockTokenPort unlockTokenPort) {
        return new CheckStorefrontLockUseCase(storefrontShopQuery, unlockTokenPort);
    }

    @Bean
    public UnlockStorefrontUseCase unlockStorefrontUseCase(
            StorefrontShopQuery storefrontShopQuery, UnlockTokenPort unlockTokenPort) {
        return new UnlockStorefrontUseCase(storefrontShopQuery, unlockTokenPort);
    }

    @Bean
    public GetPasswordPageUseCase getPasswordPageUseCase(StorefrontShopQuery storefrontShopQuery) {
        return new GetPasswordPageUseCase(storefrontShopQuery);
    }

    /**
     * The base domain is config, so the resolver is built here rather than being
     * a component: it is a policy object with one constructor argument that only
     * infrastructure knows.
     */
    @Bean
    public TenantHandleResolver tenantHandleResolver(StorefrontProperties properties) {
        return new TenantHandleResolver(properties.baseDomain());
    }
}

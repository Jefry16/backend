package com.vointika.storefront.infrastructure.config;

import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.port.UnlockTokenPort;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.GetExperienceListPageUseCase;
import com.vointika.storefront.application.usecase.GetHomePageUseCase;
import com.vointika.storefront.application.usecase.GetPasswordPageUseCase;
import com.vointika.storefront.application.usecase.GetPolicyPageUseCase;
import com.vointika.storefront.application.usecase.UnlockStorefrontUseCase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("storefrontUseCaseConfig")
@EnableConfigurationProperties(StorefrontProperties.class)
public class StorefrontUseCaseConfig {

    @Bean
    public GetHomePageUseCase getHomePageUseCase(StorefrontShopQuery storefrontShopQuery) {
        return new GetHomePageUseCase(storefrontShopQuery);
    }

    @Bean
    public GetExperienceListPageUseCase getExperienceListPageUseCase(
            StorefrontShopQuery storefrontShopQuery,
            StorefrontExperienceQuery storefrontExperienceQuery) {
        return new GetExperienceListPageUseCase(storefrontShopQuery, storefrontExperienceQuery);
    }

    @Bean
    public GetPolicyPageUseCase getPolicyPageUseCase(StorefrontShopQuery storefrontShopQuery) {
        return new GetPolicyPageUseCase(storefrontShopQuery);
    }

    @Bean
    public CheckStorefrontLockUseCase checkStorefrontLockUseCase(StorefrontShopQuery storefrontShopQuery,
                                                                 UnlockTokenPort unlockTokenPort) {
        return new CheckStorefrontLockUseCase(storefrontShopQuery, unlockTokenPort);
    }

    @Bean
    public UnlockStorefrontUseCase unlockStorefrontUseCase(StorefrontShopQuery storefrontShopQuery,
                                                           UnlockTokenPort unlockTokenPort) {
        return new UnlockStorefrontUseCase(storefrontShopQuery, unlockTokenPort);
    }

    @Bean
    public GetPasswordPageUseCase getPasswordPageUseCase(StorefrontShopQuery storefrontShopQuery) {
        return new GetPasswordPageUseCase(storefrontShopQuery);
    }

    @Bean
    public TenantHandleResolver tenantHandleResolver(StorefrontProperties properties) {
        return new TenantHandleResolver(properties.baseDomain());
    }
}

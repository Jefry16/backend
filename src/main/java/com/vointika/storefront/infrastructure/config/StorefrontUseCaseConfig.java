package com.vointika.storefront.infrastructure.config;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.GetHomePageUseCase;
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
    public TenantHandleResolver tenantHandleResolver(StorefrontProperties properties) {
        return new TenantHandleResolver(properties.baseDomain());
    }
}

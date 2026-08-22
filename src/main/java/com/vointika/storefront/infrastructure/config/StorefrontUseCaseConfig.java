package com.vointika.storefront.infrastructure.config;

import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontMenuQuery;
import com.vointika.shared.port.StorefrontPageQuery;
import com.vointika.shared.port.StorefrontMetafieldQuery;
import com.vointika.shared.port.StorefrontTourOperatorQuery;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.port.UnlockTokenPort;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.GetPasswordPageUseCase;
import com.vointika.storefront.application.usecase.GetStorefrontCmsPageUseCase;
import com.vointika.storefront.application.usecase.GetStorefrontExperienceUseCase;
import com.vointika.storefront.application.usecase.GetStorefrontPolicyUseCase;
import com.vointika.storefront.application.usecase.GetStorefrontExperienceListUseCase;
import com.vointika.storefront.application.usecase.GetStorefrontGlobalsUseCase;
import com.vointika.storefront.application.usecase.UnlockStorefrontUseCase;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Use cases are plain POJOs hand-wired here, as everywhere.
 *
 * <p><b>{@link GetStorefrontGlobalsUseCase} is a collaborator of every other page
 * use case, never a superclass.</b> The globals are identical on every route, so
 * a page's own read is a second query beside them rather than a branch inside
 * one — which is also why the home page wires no use case of its own.
 *
 * <p>The rest of the beans here are the password gate's, and they are separate
 * from the pages for the reason the gate is: it runs before locale resolution.
 */
@Configuration("storefrontUseCaseConfig")
@EnableConfigurationProperties(StorefrontProperties.class)
public class StorefrontUseCaseConfig {


    @Bean
    public GetStorefrontGlobalsUseCase getStorefrontGlobalsUseCase(
            StorefrontTourOperatorQuery storefrontTourOperatorQuery,
            StorefrontMetafieldQuery storefrontMetafieldQuery,
            StorefrontExperienceQuery storefrontExperienceQuery,
            StorefrontMenuQuery storefrontMenuQuery,
            StorefrontPageQuery storefrontPageQuery) {
        return new GetStorefrontGlobalsUseCase(storefrontTourOperatorQuery, storefrontMetafieldQuery,
                storefrontExperienceQuery, storefrontMenuQuery, storefrontPageQuery);
    }

    @Bean
    public GetStorefrontExperienceListUseCase getStorefrontExperienceListUseCase(
            GetStorefrontGlobalsUseCase getStorefrontGlobalsUseCase,
            StorefrontExperienceQuery storefrontExperienceQuery) {
        return new GetStorefrontExperienceListUseCase(getStorefrontGlobalsUseCase, storefrontExperienceQuery);
    }

    @Bean
    public GetStorefrontPolicyUseCase getStorefrontPolicyUseCase(
            GetStorefrontGlobalsUseCase getStorefrontGlobalsUseCase,
            StorefrontTourOperatorQuery storefrontTourOperatorQuery) {
        return new GetStorefrontPolicyUseCase(getStorefrontGlobalsUseCase, storefrontTourOperatorQuery);
    }

    @Bean
    public GetStorefrontExperienceUseCase getStorefrontExperienceUseCase(
            GetStorefrontGlobalsUseCase getStorefrontGlobalsUseCase,
            StorefrontExperienceQuery storefrontExperienceQuery,
            StorefrontMetafieldQuery storefrontMetafieldQuery) {
        return new GetStorefrontExperienceUseCase(getStorefrontGlobalsUseCase,
                storefrontExperienceQuery, storefrontMetafieldQuery);
    }

    @Bean
    public GetStorefrontCmsPageUseCase getStorefrontCmsPageUseCase(
            GetStorefrontGlobalsUseCase getStorefrontGlobalsUseCase,
            StorefrontPageQuery storefrontPageQuery) {
        return new GetStorefrontCmsPageUseCase(getStorefrontGlobalsUseCase, storefrontPageQuery);
    }

    @Bean
    public CheckStorefrontLockUseCase checkStorefrontLockUseCase(
            StorefrontTourOperatorQuery storefrontTourOperatorQuery, UnlockTokenPort unlockTokenPort) {
        return new CheckStorefrontLockUseCase(storefrontTourOperatorQuery, unlockTokenPort);
    }

    @Bean
    public UnlockStorefrontUseCase unlockStorefrontUseCase(
            StorefrontTourOperatorQuery storefrontTourOperatorQuery, UnlockTokenPort unlockTokenPort) {
        return new UnlockStorefrontUseCase(storefrontTourOperatorQuery, unlockTokenPort);
    }

    @Bean
    public GetPasswordPageUseCase getPasswordPageUseCase(StorefrontTourOperatorQuery storefrontTourOperatorQuery) {
        return new GetPasswordPageUseCase(storefrontTourOperatorQuery);
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

package com.vointika.rendering.infrastructure.config;

import com.vointika.rendering.application.usecase.GetExperienceListRenderContextUseCase;
import com.vointika.rendering.application.usecase.GetExperienceRenderContextUseCase;
import com.vointika.rendering.application.service.NavigationAssembler;
import com.vointika.rendering.application.service.TenantResolver;
import com.vointika.rendering.application.usecase.GetPageRenderContextUseCase;
import com.vointika.rendering.application.usecase.GetShopRenderContextUseCase;
import com.vointika.rendering.application.usecase.VerifyStorefrontPasswordUseCase;
import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontOperatorQuery;
import com.vointika.shared.port.StorefrontNavigationQuery;
import com.vointika.shared.port.StorefrontPageQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("renderingUseCaseConfig")
public class RenderingUseCaseConfig {

    @Bean
    public NavigationAssembler navigationAssembler(StorefrontNavigationQuery navigationQuery,
                                                   StorefrontExperienceQuery experienceQuery,
                                                   StorefrontPageQuery pageQuery) {
        return new NavigationAssembler(navigationQuery, experienceQuery, pageQuery);
    }

    @Bean
    public TenantResolver tenantResolver(StorefrontOperatorQuery storefrontOperatorQuery,
                                         NavigationAssembler navigationAssembler) {
        return new TenantResolver(storefrontOperatorQuery, navigationAssembler);
    }

    @Bean
    public GetShopRenderContextUseCase getShopRenderContextUseCase(TenantResolver tenantResolver) {
        return new GetShopRenderContextUseCase(tenantResolver);
    }

    @Bean
    public GetExperienceListRenderContextUseCase getExperienceListRenderContextUseCase(
            TenantResolver tenantResolver,
            StorefrontExperienceQuery storefrontExperienceQuery) {
        return new GetExperienceListRenderContextUseCase(tenantResolver, storefrontExperienceQuery);
    }

    @Bean
    public GetExperienceRenderContextUseCase getExperienceRenderContextUseCase(
            TenantResolver tenantResolver,
            StorefrontExperienceQuery storefrontExperienceQuery) {
        return new GetExperienceRenderContextUseCase(tenantResolver, storefrontExperienceQuery);
    }

    @Bean
    public GetPageRenderContextUseCase getPageRenderContextUseCase(
            TenantResolver tenantResolver, StorefrontPageQuery storefrontPageQuery) {
        return new GetPageRenderContextUseCase(tenantResolver, storefrontPageQuery);
    }

    @Bean
    public VerifyStorefrontPasswordUseCase verifyStorefrontPasswordUseCase(
            StorefrontOperatorQuery storefrontOperatorQuery) {
        return new VerifyStorefrontPasswordUseCase(storefrontOperatorQuery);
    }
}

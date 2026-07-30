package com.vointika.rendering.infrastructure.config;

import com.vointika.rendering.application.usecase.GetExperienceListRenderContextUseCase;
import com.vointika.rendering.application.usecase.GetExperienceRenderContextUseCase;
import com.vointika.rendering.application.usecase.GetShopRenderContextUseCase;
import com.vointika.rendering.application.usecase.VerifyStorefrontPasswordUseCase;
import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontOperatorQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("renderingUseCaseConfig")
public class RenderingUseCaseConfig {

    @Bean
    public GetShopRenderContextUseCase getShopRenderContextUseCase(
            StorefrontOperatorQuery storefrontOperatorQuery) {
        return new GetShopRenderContextUseCase(storefrontOperatorQuery);
    }

    @Bean
    public GetExperienceListRenderContextUseCase getExperienceListRenderContextUseCase(
            StorefrontOperatorQuery storefrontOperatorQuery,
            StorefrontExperienceQuery storefrontExperienceQuery) {
        return new GetExperienceListRenderContextUseCase(storefrontOperatorQuery, storefrontExperienceQuery);
    }

    @Bean
    public GetExperienceRenderContextUseCase getExperienceRenderContextUseCase(
            StorefrontOperatorQuery storefrontOperatorQuery,
            StorefrontExperienceQuery storefrontExperienceQuery) {
        return new GetExperienceRenderContextUseCase(storefrontOperatorQuery, storefrontExperienceQuery);
    }

    @Bean
    public VerifyStorefrontPasswordUseCase verifyStorefrontPasswordUseCase(
            StorefrontOperatorQuery storefrontOperatorQuery) {
        return new VerifyStorefrontPasswordUseCase(storefrontOperatorQuery);
    }
}

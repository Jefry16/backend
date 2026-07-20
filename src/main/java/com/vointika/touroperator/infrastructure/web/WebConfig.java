package com.vointika.touroperator.infrastructure.web;

import com.vointika.shared.port.TourOperatorMembershipCheck;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The membership check is injected as an {@link ObjectProvider} (resolved
 * per-request by the interceptor) so this WebMvcConfigurer constructs even in a
 * {@code @WebMvcTest} slice that doesn't supply the bean — the interceptor only
 * resolves it when a {@code /api/tour-operators/{id}/**} route actually matches.
 */
@Configuration("tourOperatorWebConfig")
public class WebConfig implements WebMvcConfigurer {

    private final ObjectProvider<TourOperatorMembershipCheck> membershipCheck;

    public WebConfig(ObjectProvider<TourOperatorMembershipCheck> membershipCheck) {
        this.membershipCheck = membershipCheck;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TourOperatorMembershipInterceptor(membershipCheck))
                .addPathPatterns("/api/tour-operators/**");
    }
}

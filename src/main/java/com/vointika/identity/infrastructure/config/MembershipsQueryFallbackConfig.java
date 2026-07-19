package com.vointika.identity.infrastructure.config;

import com.vointika.shared.port.UserTourOperatorMembershipsQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * TEMPORARY fallback for {@link UserTourOperatorMembershipsQuery} until the
 * {@code touroperator} context lands and provides the real implementation.
 *
 * <p>{@code GetProfileUseCase} joins the caller's operator memberships into the
 * profile response. A brand-new user has no memberships, so an empty list is a
 * truthful answer for now and keeps the response shape unchanged. Guarded by
 * {@code @ConditionalOnMissingBean} so touroperator's real implementation wins
 * automatically once it exists.
 *
 * <p><b>DELETE this class when the touroperator context is built</b> (tracked in
 * MAP.md).
 */
@Configuration
public class MembershipsQueryFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(UserTourOperatorMembershipsQuery.class)
    public UserTourOperatorMembershipsQuery emptyUserTourOperatorMembershipsQuery() {
        return userId -> List.of();
    }
}

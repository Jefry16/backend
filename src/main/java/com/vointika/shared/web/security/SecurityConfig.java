package com.vointika.shared.web.security;

import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.RateLimiterPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({SecurityProperties.class, StorefrontApiProperties.class})
public class SecurityConfig {

    private final AccessTokenValidatorPort accessTokenValidator;
    private final List<PublicRouteRegistrar> publicRouteRegistrars;
    private final StorefrontApiProperties storefrontApiProperties;
    private final ObjectProvider<RateLimiterPort> rateLimiterProvider;
    private final boolean rateLimitEnabled;

    public SecurityConfig(AccessTokenValidatorPort accessTokenValidator,
                          List<PublicRouteRegistrar> publicRouteRegistrars,
                          StorefrontApiProperties storefrontApiProperties,
                          ObjectProvider<RateLimiterPort> rateLimiterProvider,
                          @Value("${app.rate-limit.enabled:true}") boolean rateLimitEnabled) {
        this.accessTokenValidator = accessTokenValidator;
        this.publicRouteRegistrars = publicRouteRegistrars;
        this.storefrontApiProperties = storefrontApiProperties;
        this.rateLimiterProvider = rateLimiterProvider;
        this.rateLimitEnabled = rateLimitEnabled;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint()))
                .authorizeHttpRequests(auth -> {
                    for (PublicRouteRegistrar registrar : publicRouteRegistrars) {
                        for (PublicRoute route : registrar.publicRoutes()) {
                            auth.requestMatchers(route.method(), route.pattern()).permitAll();
                        }
                    }
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(
                        new StorefrontApiSecretFilter(storefrontApiProperties.sharedSecret()),
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(accessTokenValidator),
                        UsernamePasswordAuthenticationFilter.class
                )
                // After the JWT filter, so the principal is populated: the
                // coarse per-user runaway-script cap (§7.9 third layer).
                .addFilterAfter(
                        new ApiRateLimitFilter(rateLimiterProvider, rateLimitEnabled),
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }
}
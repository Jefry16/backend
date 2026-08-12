package com.vointika.storefront.presentation.controller;

import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.CheckStorefrontTenantUseCase;
import com.vointika.storefront.infrastructure.security.StorefrontPublicRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * No test here sends an {@code Authorization} header and every one expects a
 * body: the storefront is public, and importing {@link StorefrontPublicRoutes}
 * is what proves it. Omit that import and every request 401s, so the assertions
 * would pass without testing anything (PATTERNS §8c).
 *
 * <p>What is worth pinning while the storefront is a placeholder is exactly what
 * survived the cutback — the host still decides whether there is a storefront
 * here at all.
 *
 * <p><b>The home addresses are no longer here.</b> {@code /} and {@code /{locale}}
 * moved to {@code StorefrontHomeController} when the globals landed; what is left
 * is the routes that still have no page.
 */
@WebMvcTest(StorefrontPlaceholderController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class})
class StorefrontPlaceholderControllerTest {

    private static final String[] EVERY_ADDRESS = {
            StorefrontRoutes.EXPERIENCES, "/es/experiences",
            "/policies/terms", "/es/policies/cancellation"};

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private CheckStorefrontTenantUseCase checkStorefrontTenantUseCase;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;
    /**
     * <b>Required, not incidental.</b> {@code StorefrontWebConfig} is a
     * {@code WebMvcConfigurer}, so every {@code @WebMvcTest} slice registers the
     * gate's interceptor — and it resolves this use case per request. Leave it
     * out and every storefront request is a 500, the same way a slice missing
     * {@code TourOperatorMembershipCheck} 500s the admin API.
     */
    @MockitoBean private CheckStorefrontLockUseCase checkStorefrontLockUseCase;

    @BeforeEach
    void storefrontIsOpen() {
        when(checkStorefrontLockUseCase.execute(anyString(), any())).thenReturn(LockState.UNLOCKED);
    }

    private void tenantIs(String host, String handle, boolean exists) {
        when(tenantHandleResolver.resolve(host)).thenReturn(Optional.of(handle));
        when(checkStorefrontTenantUseCase.execute(handle)).thenReturn(exists);
    }

    /**
     * The stubbed host has <em>no port</em> while the request's does: the
     * controller reads {@code getServerName()}, not the raw {@code Host} header,
     * and {@code MockHttpServletRequest} derives the former from the latter with
     * the port stripped. Read the raw header instead and this stub misses, the
     * mock answers empty, and this fails on a 404 — so it is the regression guard
     * for that choice, which matters because {@code ForwardedHeaderFilter} makes
     * the servlet API honour {@code X-Forwarded-Host}.
     */
    @Test
    void servesThePlaceholderForARealTenant() throws Exception {
        tenantIs("acme.localhost", "acme", true);

        mockMvc.perform(get(StorefrontRoutes.EXPERIENCES).header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.handle").value("acme"))
                .andExpect(jsonPath("$.status").value("coming-soon"));
    }

    /**
     * Tenant resolution is the half that survived, so it still has to answer
     * honestly — and it answers in the application's one error shape, because the
     * controller throws {@code ResourceNotFoundException} rather than building a
     * body of its own.
     */
    @Test
    void aHandleNoOperatorOwnsIs404() throws Exception {
        tenantIs("nope.localhost", "nope", false);

        mockMvc.perform(get(StorefrontRoutes.EXPERIENCES).header("Host", "nope.localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("There is no storefront at this address"));
    }

    /** The apex names no tenant — {@link TenantHandleResolver} answers empty and nothing is looked up. */
    @Test
    void theApexIs404() throws Exception {
        when(tenantHandleResolver.resolve("localhost")).thenReturn(Optional.empty());

        mockMvc.perform(get(StorefrontRoutes.EXPERIENCES).header("Host", "localhost:8080"))
                .andExpect(status().isNotFound());
    }

    /**
     * Every address the storefront owns still resolves, and all of them answer the
     * same body for now. The addresses are the expensive part to get right again;
     * the pages are not.
     */
    @Test
    void everyStorefrontAddressServesThePlaceholder() throws Exception {
        tenantIs("acme.localhost", "acme", true);

        for (String path : EVERY_ADDRESS) {
            mockMvc.perform(get(path).header("Host", "acme.localhost:8080"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.handle").value("acme"));
        }
    }

    /**
     * Spring MVC serves HEAD from a {@code @GetMapping} for free; Spring Security
     * does not, and rejects an unlisted method at the filter chain as a 401 in the
     * JSON error shape before MVC is reached. Harmless on a JSON API nobody HEADs,
     * wrong on a public address — crawlers, link checkers, uptime monitors and CDNs
     * all send HEAD. It took a request against the built stack to find last time.
     */
    @Test
    void servesHeadAsWellAsGet() throws Exception {
        tenantIs("acme.localhost", "acme", true);

        for (String path : EVERY_ADDRESS) {
            mockMvc.perform(head(path).header("Host", "acme.localhost:8080"))
                    .andExpect(status().isOk());
        }
    }

    /**
     * The constrained locale variable is a security decision, not a routing one: a
     * bare {@code /{locale}} {@code permitAll}s <em>every single-segment path in
     * the application</em>, and review of #91 measured {@code /error} going
     * 401 → 200 when it was unconstrained.
     *
     * <p><b>401 is the pass condition here</b>, not a bug: it means the storefront's
     * route never claimed {@code /error}, so the filter chain refuses it like any
     * other non-public path. Unconstrain the variable and this turns 200 — the
     * placeholder served on a path the storefront does not own.
     */
    @Test
    void aSegmentThatIsNotLocaleShapedIsNotAStorefrontPage() throws Exception {
        tenantIs("acme.localhost", "acme", true);

        mockMvc.perform(get("/error").header("Host", "acme.localhost:8080"))
                .andExpect(status().isUnauthorized());
    }
}

package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.PasswordPageOutput;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.port.UnlockTokenPort;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.GetHomePageUseCase;
import com.vointika.storefront.application.usecase.GetPasswordPageUseCase;
import com.vointika.storefront.application.usecase.UnlockStorefrontUseCase;
import com.vointika.storefront.infrastructure.config.StorefrontMustacheConfig;
import com.vointika.storefront.infrastructure.security.StorefrontPublicRoutes;
import com.vointika.storefront.infrastructure.web.StorefrontWebConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Both storefront controllers are registered here on purpose. {@code /{locale}}
 * matches the literal {@code /password} too, and only Spring's
 * {@code PathPattern} preferring the literal keeps the password page reachable —
 * a property of the matcher, so the next top-level literal route should break a
 * test rather than a page.
 *
 * <p>The gate does not cover {@code /password}: this is where a locked store
 * sends its visitors, so gating it would loop. The tests below run against a
 * locked store to prove exactly that.
 */
@WebMvcTest({PasswordPageController.class, StorefrontHomeController.class})
@Import({SecurityConfig.class, StorefrontPublicRoutes.class, StorefrontMustacheConfig.class,
        StorefrontWebConfig.class})
class PasswordPageControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetPasswordPageUseCase getPasswordPageUseCase;
    @MockitoBean private UnlockStorefrontUseCase unlockStorefrontUseCase;
    @MockitoBean private CheckStorefrontLockUseCase checkStorefrontLockUseCase;
    @MockitoBean private GetHomePageUseCase getHomePageUseCase;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

    @BeforeEach
    void aLockedStoreCalledAcme() {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(checkStorefrontLockUseCase.execute(any(), any())).thenReturn(LockState.LOCKED);
        when(getPasswordPageUseCase.execute("acme")).thenReturn(Optional.of(
                new PasswordPageOutput("Acme Tours", "Volvemos pronto")));
    }

    @Test
    void rendersTheFormWithTheShopNameAndTheOperatorsMessage() throws Exception {
        mockMvc.perform(get("/password").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Acme Tours</h1>"),
                        containsString("Volvemos pronto"),
                        containsString("<form method=\"post\" action=\"/password\">"),
                        not(containsString("That password is not right")))));
    }

    @Test
    void servesHeadAsWellAsGet() throws Exception {
        mockMvc.perform(head("/password").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void theRightPasswordSetsTheCookieAndRedirectsHome() throws Exception {
        when(unlockStorefrontUseCase.execute("acme", "hunter2")).thenReturn(Optional.of("a-valid-token"));

        mockMvc.perform(post("/password").header("Host", "acme.localhost").param("password", "hunter2"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/"))
                .andExpect(cookie().value(UnlockTokenPort.COOKIE_NAME, "a-valid-token"))
                .andExpect(cookie().httpOnly(UnlockTokenPort.COOKIE_NAME, true))
                .andExpect(cookie().path(UnlockTokenPort.COOKIE_NAME, "/"))
                // A session cookie: closing the browser re-locks the store.
                .andExpect(cookie().maxAge(UnlockTokenPort.COOKIE_NAME, -1))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }

    /** Plain HTTP in dev must still work, so {@code Secure} follows the request. */
    @Test
    void theCookieIsSecureOnlyWhenTheRequestIs() throws Exception {
        when(unlockStorefrontUseCase.execute("acme", "hunter2")).thenReturn(Optional.of("a-valid-token"));

        mockMvc.perform(post("/password").header("Host", "acme.localhost").param("password", "hunter2"))
                .andExpect(cookie().secure(UnlockTokenPort.COOKIE_NAME, false));
        mockMvc.perform(post("/password").secure(true).header("Host", "acme.localhost")
                        .param("password", "hunter2"))
                .andExpect(cookie().secure(UnlockTokenPort.COOKIE_NAME, true));
    }

    @Test
    void theWrongPasswordRerendersWithTheErrorAndNoCookie() throws Exception {
        when(unlockStorefrontUseCase.execute("acme", "wrong")).thenReturn(Optional.empty());

        mockMvc.perform(post("/password").header("Host", "acme.localhost").param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("That password is not right"),
                        containsString("<h1>Acme Tours</h1>"))))
                .andExpect(cookie().doesNotExist(UnlockTokenPort.COOKIE_NAME));
    }

    @Test
    void aSubmissionWithNoPasswordAtAllIsRefused() throws Exception {
        when(unlockStorefrontUseCase.execute("acme", null)).thenReturn(Optional.empty());

        mockMvc.perform(post("/password").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("That password is not right")))
                .andExpect(cookie().doesNotExist(UnlockTokenPort.COOKIE_NAME));
    }

    @Test
    void aHostThatAddressesNoTenantGetsTheNotFoundPage() throws Exception {
        when(tenantHandleResolver.resolve("localhost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/password").header("Host", "localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
        mockMvc.perform(post("/password").header("Host", "localhost:8080").param("password", "hunter2"))
                .andExpect(status().isNotFound());
    }

    /**
     * The password page must stay reachable on a locked store, or the redirect
     * the gate issues points at a page that redirects back. Drop the exclusion in
     * {@code StorefrontWebConfig} and both of these become a 302 to
     * {@code /password}.
     */
    @Test
    void theGateDoesNotCoverThePasswordPageItself() throws Exception {
        when(unlockStorefrontUseCase.execute("acme", "hunter2")).thenReturn(Optional.of("a-valid-token"));

        mockMvc.perform(get("/password").header("Host", "acme.localhost"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/password").header("Host", "acme.localhost").param("password", "hunter2"))
                .andExpect(header().string("Location", "/"));
    }

    /**
     * <b>Trap 1.</b> {@code /{locale}} matches {@code /password} as well;
     * {@code PathPattern} prefers the literal, so the password controller wins.
     * Nothing in this codebase chose that — the matcher did — so it is asserted
     * rather than assumed, and the home page is never asked for a locale called
     * "password".
     */
    @Test
    void thePasswordPathReachesThePasswordControllerAndNotTheLocaleRoute() throws Exception {
        mockMvc.perform(get("/password").header("Host", "acme.localhost"))
                .andExpect(content().string(containsString("<form method=\"post\" action=\"/password\">")));
        verify(getHomePageUseCase, never()).execute(any(), any());
    }
}

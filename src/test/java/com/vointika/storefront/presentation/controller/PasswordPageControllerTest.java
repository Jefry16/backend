package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.PasswordPageOutput;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.port.UnlockTokenPort;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.GetPasswordPageUseCase;
import com.vointika.storefront.application.usecase.GetStorefrontGlobalsUseCase;
import com.vointika.storefront.application.usecase.UnlockStorefrontUseCase;
import com.vointika.storefront.infrastructure.security.StorefrontPublicRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The gate's own address. Both controllers are in the slice because one of the
 * assertions is about which of them answers {@code /password} at all.
 */
@WebMvcTest({PasswordPageController.class, StorefrontHomeController.class})
@Import({SecurityConfig.class, StorefrontPublicRoutes.class})
class PasswordPageControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetPasswordPageUseCase getPasswordPageUseCase;
    @MockitoBean private UnlockStorefrontUseCase unlockStorefrontUseCase;
    @MockitoBean private CheckStorefrontLockUseCase checkStorefrontLockUseCase;
    @MockitoBean private GetStorefrontGlobalsUseCase getStorefrontGlobalsUseCase;
    @MockitoBean private MediaAssetBatchQuery mediaAssetBatchQuery;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

    @BeforeEach
    void setUp() {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getPasswordPageUseCase.execute("acme"))
                .thenReturn(Optional.of(new PasswordPageOutput("Acme Tours", "We open on Monday")));
        // The store this suite describes is locked; that is the only state in
        // which a visitor sees this page at all.
        when(checkStorefrontLockUseCase.execute(anyString(), any())).thenReturn(LockState.LOCKED);
    }

    @Test
    void theGateShowsTheOperatorNameAndItsMessage() throws Exception {
        mockMvc.perform(get("/password").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operatorName").value("Acme Tours"))
                .andExpect(jsonPath("$.passwordMessage").value("We open on Monday"))
                .andExpect(jsonPath("$.error").value(false));
    }

    /**
     * <b>The gate is not gated.</b> The redirect points here, so registering the
     * interceptor on this path would loop a locked store forever. The stub above
     * says LOCKED for every path, so a 200 here is the proof.
     */
    @Test
    void theGateItselfIsNotBehindTheGate() throws Exception {
        mockMvc.perform(get("/password").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk());
    }

    /**
     * {@code /{locale}} matches the literal {@code /password} too — a two-letter
     * segment is a two-letter segment. {@code PathPattern} prefers the literal,
     * so this works; it is asserted because every top-level literal route added
     * later inherits the same collision, and the failure would be a password page
     * quietly served as a home page.
     */
    @Test
    void theLiteralRouteWinsOverTheLocalePattern() throws Exception {
        mockMvc.perform(get("/password").header("Host", "acme.localhost:8080"))
                .andExpect(jsonPath("$.operatorName").exists())
                .andExpect(jsonPath("$.tourOperator").doesNotExist());
    }

    /**
     * A right password sets the cookie and redirects. It is a redirect and not a
     * JSON body because the eventual consumer is an HTML form, and deciding that
     * now costs nothing.
     */
    @Test
    void theRightPasswordSetsTheCookieAndRedirectsHome() throws Exception {
        when(unlockStorefrontUseCase.execute("acme", "hunter2")).thenReturn(Optional.of("a-token"));

        mockMvc.perform(post("/password").header("Host", "acme.localhost:8080").param("password", "hunter2"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/"))
                .andExpect(cookie().value(UnlockTokenPort.COOKIE_NAME, "a-token"))
                .andExpect(cookie().httpOnly(UnlockTokenPort.COOKIE_NAME, true));
    }

    /** Session cookie, so closing the browser re-locks the store. */
    @Test
    void theUnlockCookieDoesNotOutliveTheBrowserSession() throws Exception {
        when(unlockStorefrontUseCase.execute("acme", "hunter2")).thenReturn(Optional.of("a-token"));

        mockMvc.perform(post("/password").header("Host", "acme.localhost:8080").param("password", "hunter2"))
                .andExpect(cookie().maxAge(UnlockTokenPort.COOKIE_NAME, -1));
    }

    @Test
    void aWrongPasswordReShowsTheGateWithAnErrorAndNoCookie() throws Exception {
        when(unlockStorefrontUseCase.execute("acme", "wrong")).thenReturn(Optional.empty());

        mockMvc.perform(post("/password").header("Host", "acme.localhost:8080").param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.operatorName").value("Acme Tours"))
                .andExpect(cookie().doesNotExist(UnlockTokenPort.COOKIE_NAME));
    }

    /** An empty submission is refused like a wrong one, not a 400. */
    @Test
    void anEmptySubmissionIsRefusedTheSameWay() throws Exception {
        when(unlockStorefrontUseCase.execute("acme", null)).thenReturn(Optional.empty());

        mockMvc.perform(post("/password").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(true));
    }

    @Test
    void aHostNoOperatorOwnsIs404OnBothMethods() throws Exception {
        when(tenantHandleResolver.resolve("nope.localhost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/password").header("Host", "nope.localhost:8080"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/password").header("Host", "nope.localhost:8080").param("password", "x"))
                .andExpect(status().isNotFound());
    }
}

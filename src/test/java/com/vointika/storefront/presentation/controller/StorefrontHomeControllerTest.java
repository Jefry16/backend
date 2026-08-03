package com.vointika.storefront.presentation.controller;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.shared.web.security.SecurityConfig;
import com.vointika.storefront.application.dto.output.HomePageOutput;
import com.vointika.storefront.application.policy.LocaleResolver;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import com.vointika.storefront.application.port.UnlockTokenPort;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import com.vointika.storefront.application.usecase.GetHomePageUseCase;
import com.vointika.storefront.infrastructure.config.StorefrontMustacheConfig;
import com.vointika.storefront.infrastructure.security.StorefrontPublicRoutes;
import com.vointika.storefront.infrastructure.web.StorefrontWebConfig;
import jakarta.servlet.http.Cookie;
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
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * No test here sends an {@code Authorization} header, and every one of them
 * expects a page — the storefront is public, and that is part of what this class
 * proves. The rest is that the compiler configured in
 * {@link StorefrontMustacheConfig} renders operator-authored text safely, and
 * that the gate registered by {@link StorefrontWebConfig} runs in front of the
 * locale rule rather than after it.
 */
@WebMvcTest(StorefrontHomeController.class)
@Import({SecurityConfig.class, StorefrontPublicRoutes.class, StorefrontMustacheConfig.class,
        StorefrontWebConfig.class})
class StorefrontHomeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TenantHandleResolver tenantHandleResolver;
    @MockitoBean private GetHomePageUseCase getHomePageUseCase;
    @MockitoBean private CheckStorefrontLockUseCase checkStorefrontLockUseCase;
    @MockitoBean private MediaUrlResolver mediaUrlResolver;
    @MockitoBean private AccessTokenValidatorPort accessTokenValidator;

    @BeforeEach
    void unlockedByDefault() {
        when(checkStorefrontLockUseCase.execute(any(), any())).thenReturn(LockState.UNLOCKED);
    }

    /**
     * The stubbed host has <em>no port</em> while the request's does: the
     * controller reads {@code getServerName()}, not the raw {@code Host} header,
     * and {@code MockHttpServletRequest} derives the former from the latter with
     * the port stripped. Read the raw header instead and this stub misses, the
     * mock returns empty, and the assertions below fail on a 404 — so this test
     * is the regression guard for that choice.
     */
    @Test
    void rendersTheShopForAKnownHost() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(new HomePageOutput(
                "es", "Acme Tours - day trips", "Acme Tours", "Boat tours and day trips",
                "tour-operators/1/logo.png", "tour-operators/1/og.png")));
        when(mediaUrlResolver.toUrl("tour-operators/1/logo.png"))
                .thenReturn("http://localhost:9000/avatars/tour-operators/1/logo.png");
        when(mediaUrlResolver.toUrl("tour-operators/1/og.png"))
                .thenReturn("http://localhost:9000/avatars/tour-operators/1/og.png");

        mockMvc.perform(get("/").header("Host", "acme.localhost:8080"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(allOf(
                        containsString("<html lang=\"es\">"),
                        containsString("<title>Acme Tours - day trips</title>"),
                        containsString("<h1>Acme Tours</h1>"),
                        containsString("Boat tours and day trips"),
                        containsString("<img src=\"http://localhost:9000/avatars/tour-operators/1/logo.png\""),
                        containsString("<meta property=\"og:type\" content=\"website\">"),
                        containsString("<meta property=\"og:image\" "
                                + "content=\"http://localhost:9000/avatars/tour-operators/1/og.png\">"))));
    }

    /**
     * A published secondary is served under its own prefix, in its own language,
     * and says so in {@code lang} — the reason the locale reaches the view at all.
     */
    @Test
    void rendersASecondaryLocaleUnderItsPrefix() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", "en")).thenReturn(Optional.of(new HomePageOutput(
                "en", "Acme Tours - day trips", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/en").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<html lang=\"en\">"),
                        containsString("<title>Acme Tours - day trips</title>"))));
    }

    /**
     * The primary locale and an unsupported one are the same answer, and the use
     * case decides which is which — this pins that the controller passes the path
     * locale through and renders the not-found page for an empty result.
     */
    @Test
    void answersNotFoundForALocaleThatAddressesNoPage() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", "es")).thenReturn(Optional.empty());
        when(getHomePageUseCase.execute("acme", "de")).thenReturn(Optional.empty());

        mockMvc.perform(get("/es").header("Host", "acme.localhost"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
        mockMvc.perform(get("/de").header("Host", "acme.localhost"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
    }

    /**
     * An operator who has filled in nothing optional still gets a page. Without
     * the compiler's {@code defaultValue("")} this is a 500, not a bare page.
     */
    @Test
    void omitsTheOptionalTagsWhenNothingIsSet() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(
                new HomePageOutput("es", "Acme Tours", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<h1>Acme Tours</h1>"),
                        not(containsString("og:image")),
                        not(containsString("<img")),
                        not(containsString("name=\"description\"")))));
    }

    /**
     * <b>The one assertion in this class that can see whitespace.</b> Every other
     * one is {@code containsString}, so none of them can observe what the layout
     * actually risks: whitespace between a block tag and its content is output
     * verbatim (STACK.md), and a reformat of {@code home.mustache} that looks
     * tidier ships blank lines into every page. Pinning the seam — where the
     * block's last line meets the layout's closing tags — is what catches that.
     *
     * <p>Found in review by diffing the rendered page against {@code main}, which
     * is also how the extra newline after {@code </html>} turned up: the parent's
     * close tag owns the child template's own line ending, so the output is the
     * old one plus that byte and nothing here could have said so.
     */
    @Test
    void theLayoutJoinsThePageWithoutLeakingWhitespace() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(new HomePageOutput(
                "es", "Acme Tours", "Acme Tours", null, "logo.png", null)));
        when(mediaUrlResolver.toUrl("logo.png")).thenReturn("http://localhost:9000/logo.png");

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        startsWith("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n"),
                        containsString("<body>\n    <img src=\"http://localhost:9000/logo.png\" alt=\"Acme Tours\">\n"
                                + "    <h1>Acme Tours</h1>\n</body>\n</html>"))));
    }

    /**
     * HEAD has to be registered alongside GET in {@link StorefrontPublicRoutes}:
     * Spring MVC serves it from the {@code @GetMapping} for free, but Spring
     * Security matches on the exact method and would reject it at the filter
     * chain — a 401 in a JSON shape, on a public page. Drop either HEAD entry and
     * this fails on the status. Crawlers, link checkers and uptime monitors all
     * send HEAD, and the home slice shipped without noticing because every other
     * test and curl used GET.
     */
    @Test
    void servesHeadAsWellAsGet() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(
                new HomePageOutput("es", "Acme Tours", "Acme Tours", null, null, null)));
        when(getHomePageUseCase.execute("acme", "en")).thenReturn(Optional.of(
                new HomePageOutput("en", "Acme Tours", "Acme Tours", null, null, null)));

        mockMvc.perform(head("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
        mockMvc.perform(head("/en").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    /**
     * <b>The locale route is a security pattern as well as a route.</b> It is
     * registered verbatim as a {@code PublicRoute}, so a bare {@code /{locale}}
     * would {@code permitAll} every single-segment path in the application —
     * {@code /error} and {@code /favicon.ico} today, and whatever {@code /health}
     * or {@code /metrics} someone adds later, silently and with nothing failing.
     * Loosen {@link LocaleResolver#PATH_TEMPLATE} to {@code /&#123;locale&#125;}
     * and these stop being 401.
     */
    @Test
    void theLocaleRouteDoesNotOpenEveryOtherSingleSegmentPath() throws Exception {
        when(tenantHandleResolver.resolve(any())).thenReturn(Optional.of("acme"));

        for (String path : new String[]{"/error", "/favicon.ico", "/health", "/metrics", "/api"}) {
            mockMvc.perform(get(path).header("Host", "acme.localhost"))
                    .andExpect(status().isUnauthorized());
        }
    }

    /** A locale-shaped path still routes, including the regional form the template leaves room for. */
    @Test
    void theConstrainedTemplateStillMatchesARealLocale() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", "pt-br")).thenReturn(Optional.of(
                new HomePageOutput("pt-br", "Acme Tours", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/pt-br").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"pt-br\">")));
    }

    /**
     * The gate covers the storefront's own pages, not the container's error
     * dispatch: a genuine 500 on a locked store has to stay a 500 rather than
     * being rewritten into a redirect to {@code /password}. Widen the
     * interceptor's patterns back to {@code /*} and this becomes a 302.
     */
    @Test
    void aLockedStoreDoesNotRedirectTheContainersErrorDispatch() throws Exception {
        when(tenantHandleResolver.resolve(any())).thenReturn(Optional.of("acme"));
        when(checkStorefrontLockUseCase.execute(any(), any())).thenReturn(LockState.LOCKED);

        mockMvc.perform(get("/error").header("Host", "acme.localhost"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void answersNotFoundForAnUnknownHandle() throws Exception {
        when(tenantHandleResolver.resolve("nope.localhost")).thenReturn(Optional.of("nope"));
        when(getHomePageUseCase.execute("nope", null)).thenReturn(Optional.empty());

        mockMvc.perform(get("/").header("Host", "nope.localhost"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
    }

    @Test
    void answersNotFoundWhenTheHostResolvesToNoTenant() throws Exception {
        when(tenantHandleResolver.resolve(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/").header("Host", "localhost:8080"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("<h1>Not found</h1>")));
    }

    @Test
    void escapesOperatorAuthoredText() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(new HomePageOutput(
                "es", "<script>alert(1)</script>", "<script>alert(1)</script>", null, null, null)));

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        not(containsString("<script>")),
                        containsString("&lt;script&gt;"))));
    }

    @Test
    void aLockedStoreRedirectsToThePasswordPage() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);

        mockMvc.perform(get("/").header("Host", "acme.localhost"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/password"));
    }

    /**
     * <b>The ordering test.</b> A locked store asked for a locale it does not
     * publish must redirect, not 404 — resolve the locale first and {@code /es}
     * would 404 while {@code /fr} redirected, which tells an anonymous visitor
     * that the store exists and which locales it has, from in front of the gate.
     * Move the gate after locale resolution and this is the test that goes red.
     */
    @Test
    void aLockedStoreRedirectsEvenForALocaleItDoesNotPublish() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);
        when(getHomePageUseCase.execute("acme", "de")).thenReturn(Optional.empty());

        mockMvc.perform(get("/de").header("Host", "acme.localhost"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/password"));
    }

    /**
     * The interceptor reads the unlock cookie by name and hands its value to the
     * gate. Rename the cookie on either side and the gate stops seeing it: the
     * value below arrives as null and the request redirects instead of rendering.
     */
    @Test
    void aValidUnlockCookieGetsThroughTheGate() throws Exception {
        when(tenantHandleResolver.resolve("acme.localhost")).thenReturn(Optional.of("acme"));
        when(checkStorefrontLockUseCase.execute("acme", null)).thenReturn(LockState.LOCKED);
        when(checkStorefrontLockUseCase.execute("acme", "a-valid-token")).thenReturn(LockState.UNLOCKED);
        when(getHomePageUseCase.execute("acme", null)).thenReturn(Optional.of(
                new HomePageOutput("es", "Acme Tours", "Acme Tours", null, null, null)));

        mockMvc.perform(get("/")
                        .header("Host", "acme.localhost")
                        .cookie(new Cookie(UnlockTokenPort.COOKIE_NAME, "a-valid-token")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>Acme Tours</h1>")));
    }

    /** A host that addresses no tenant never reaches the gate, so it 404s rather than redirecting. */
    @Test
    void theGateIsNotConsultedForAHostThatAddressesNoTenant() throws Exception {
        when(tenantHandleResolver.resolve(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/").header("Host", "localhost:8080"))
                .andExpect(status().isNotFound());
        verify(checkStorefrontLockUseCase, never()).execute(any(), any());
    }
}
